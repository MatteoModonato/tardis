package tardis.implementation;

import static jbse.algo.Util.valueString;
import static jbse.apps.run.DecisionProcedureGuidanceJDI.countNonRecursiveHits;
import static jbse.bc.Signatures.JAVA_STRING;
import static tardis.implementation.Util.bytecodeJump;
import static tardis.implementation.Util.bytecodeLoadConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jbse.algo.exc.CannotManageStateException;
import jbse.algo.exc.NotYetImplementedException;
import jbse.apps.run.DecisionProcedureGuidance;
import jbse.apps.run.DecisionProcedureGuidanceJDI;
import jbse.apps.run.GuidanceException;
import jbse.bc.Signature;
import jbse.bc.exc.InvalidClassFileFactoryClassException;
import jbse.common.exc.ClasspathException;
import jbse.common.exc.InvalidInputException;
import jbse.dec.DecisionProcedureAlgorithms;
import jbse.dec.DecisionProcedureAlwSat;
import jbse.dec.DecisionProcedureClassInit;
import jbse.dec.DecisionProcedureLICS;
import jbse.dec.DecisionProcedureSMTLIB2_AUFNIRA;
import jbse.dec.exc.DecisionException;
import jbse.jvm.Runner;
import jbse.jvm.RunnerBuilder;
import jbse.jvm.RunnerParameters;
import jbse.jvm.EngineParameters.BreadthMode;
import jbse.jvm.EngineParameters.StateIdentificationMode;
import jbse.jvm.Runner.Actions;
import jbse.jvm.exc.CannotBacktrackException;
import jbse.jvm.exc.CannotBuildEngineException;
import jbse.jvm.exc.EngineStuckException;
import jbse.jvm.exc.FailureException;
import jbse.jvm.exc.InitializationException;
import jbse.jvm.exc.NonexistingObservedVariablesException;
import jbse.mem.Objekt;
import jbse.mem.State;
import jbse.mem.State.Phase;
import jbse.mem.exc.ContradictionException;
import jbse.mem.exc.FrozenStateException;
import jbse.mem.exc.InvalidNumberOfOperandsException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.rewr.CalculatorRewriting;
import jbse.rewr.RewriterOperationOnSimplex;
import jbse.rules.ClassInitRulesRepo;
import jbse.rules.LICSRulesRepo;
import jbse.tree.StateTree.BranchPoint;
import jbse.val.Reference;
import jbse.val.ReferenceConcrete;
import jbse.val.ReferenceSymbolic;
import jbse.val.Value;
import tardis.Options;

final class RunnerPath implements AutoCloseable {
    private static final String SWITCH_CHAR = System.getProperty("os.name").toLowerCase().contains("windows") ? "/" : "-";

    private final String[] classpath;
    private final String z3Path;
    private final String targetMethodClassName;
    private final String targetMethodDescriptor;
    private final String targetMethodName;
    private final TestCase testCase;
    private final RunnerParameters commonParamsGuided;
    private final RunnerParameters commonParamsGuiding;
    private final int numberOfHits;

    public RunnerPath(Options o, EvosuiteResult item, State initialState) 
    throws DecisionException, CannotBuildEngineException, InitializationException, InvalidClassFileFactoryClassException, 
    NonexistingObservedVariablesException, ClasspathException, ContradictionException, CannotBacktrackException, 
    CannotManageStateException, ThreadStackEmptyException, EngineStuckException, FailureException, NoTargetHitException {
        final ArrayList<String> _classpath = new ArrayList<>();
        _classpath.add(o.getJBSELibraryPath().toString());
        _classpath.add(o.getEvosuitePath().toString());
        _classpath.add(o.getTmpBinDirectoryPath().toString());
        _classpath.addAll(o.getClassesPath().stream().map(Object::toString).collect(Collectors.toList()));
        this.classpath = _classpath.toArray(new String[0]);
        this.z3Path = o.getZ3Path().toString();
        this.targetMethodClassName = item.getTargetMethodClassName();
        this.targetMethodDescriptor = item.getTargetMethodDescriptor();
        this.targetMethodName = item.getTargetMethodName();
        this.testCase = item.getTestCase();

        //builds the template parameters object for the guided (symbolic) 
        //and the guiding (concrete) executions
        this.commonParamsGuided = new RunnerParameters();
        this.commonParamsGuiding = new RunnerParameters();
        fillCommonParams(o, item, initialState);

        //calculates the number of hits
        this.numberOfHits = countNumberOfInvocations(this.targetMethodClassName, this.targetMethodDescriptor, this.targetMethodName);
        if (this.numberOfHits == 0) {
            throw new NoTargetHitException();
        }
    }
    
    private void fillCommonParams(Options o, EvosuiteResult item, State initialState) {
        //builds the template parameters object for the guided (symbolic) execution
        if (initialState == null) {
            this.commonParamsGuided.addUserClasspath(this.classpath);
            this.commonParamsGuided.setMethodSignature(item.getTargetMethodClassName(), item.getTargetMethodDescriptor(), item.getTargetMethodName());
        } else {
            this.commonParamsGuided.setStartingState(initialState);
        }
        this.commonParamsGuided.setBreadthMode(BreadthMode.ALL_DECISIONS_SYMBOLIC);
        if (o.getHeapScope() != null) {
            for (Map.Entry<String, Integer> e : o.getHeapScope().entrySet()) {
                this.commonParamsGuided.setHeapScope(e.getKey(), e.getValue());
            }
        }
        if (o.getCountScope() > 0) {
            this.commonParamsGuided.setCountScope(o.getCountScope());
        }
        for (List<String> unint : o.getUninterpreted()) {
            this.commonParamsGuided.addUninterpreted(unint.get(0), unint.get(1), unint.get(2));
        }

        //builds the template parameters object for the guiding (concrete) execution
        this.commonParamsGuiding.addUserClasspath(this.classpath);
        this.commonParamsGuiding.setStateIdentificationMode(StateIdentificationMode.COMPACT);

        //more settings to the template parameters objects:
        //1- accelerate things by bypassing standard loading
        if (initialState == null) {
            this.commonParamsGuided.setBypassStandardLoading(true);
        }
        this.commonParamsGuiding.setBypassStandardLoading(true); //this has no effect with JDI guidance (unfortunately introduces misalignments between the two)

        //2- disallow aliasing to static, pre-initial objects (too hard)
        this.commonParamsGuided.setMakePreInitClassesSymbolic(false);
        this.commonParamsGuiding.setMakePreInitClassesSymbolic(false);

        //3- set the maximum length of arrays with simple representation
        this.commonParamsGuided.setMaxSimpleArrayLength(o.getMaxSimpleArrayLength());
        this.commonParamsGuiding.setMaxSimpleArrayLength(o.getMaxSimpleArrayLength());

        //4- set the guiding method (to be executed concretely)
        this.commonParamsGuiding.setMethodSignature(this.testCase.getClassName(), this.testCase.getMethodDescriptor(), this.testCase.getMethodName());
        
        //5- set the executions to execute the static initializer
        this.commonParamsGuiding.addClassInvariantAfterInitializationPattern(".*");
        this.commonParamsGuided.addClassInvariantAfterInitializationPattern(".*");
    }

    /**
     * The {@link Actions} for the {@link Runner} that runs the guided symbolic execution
     * up to the pre-frontier for a given depth.
     * 
     * @author Pietro Braione
     *
     */
    private static class ActionsRunnerPreFrontier extends Actions {
        private final DecisionProcedureGuidance guid;
        private int testDepth = 0;
        private boolean atJump = false;
        private int jumpPC = 0;
        private boolean atLoadConstant = false;
        private int loadConstantStackSize = 0;
        private final HashMap<Long, String> stringLiterals = new HashMap<>();
        private final HashSet<String> coverage = new HashSet<>();

        public ActionsRunnerPreFrontier(DecisionProcedureGuidance guid) {
            this.guid = guid;
        }
        
        public void setTestDepth(int testDepth) {
            this.testDepth = testDepth;
        }

        @Override
        public boolean atInitial() {
            if (this.testDepth == 0) {
                return true;
            } else {
                return super.atInitial();
            }
        }

        @Override
        public boolean atStepPre() {
            final State currentState = getEngine().getCurrentState();
            if (currentState.phase() != Phase.PRE_INITIAL) {
                try {
                    final int currentProgramCounter = currentState.getCurrentProgramCounter();
                    final byte currentInstruction = currentState.getInstruction();
                    
                    //if at entry of a method, add the entry point to coverage 
                    if (currentProgramCounter == 0) {
                        this.coverage.add(currentState.getCurrentMethodSignature().toString() + ":0:0");
                    }

                    //if at a jump bytecode, saves the start program counter
                    this.atJump = bytecodeJump(currentInstruction);
                    if (this.atJump) {
                        this.jumpPC = currentProgramCounter;
                    }

                    //if at a load constant bytecode, saves the stack size
                    this.atLoadConstant = bytecodeLoadConstant(currentInstruction);
                    if (this.atLoadConstant) {
                        this.loadConstantStackSize = currentState.getStackSize();
                    }
                } catch (ThreadStackEmptyException | FrozenStateException e) {
                    //this should never happen
                    throw new RuntimeException(e); //TODO better exception!
                }
            }
            
            //stops if at the pre-frontier
            if (currentState.getDepth() == this.testDepth) {
                return true;
            } else {
                return super.atStepPre();
            }
        }

        @Override
        public boolean atStepPost() {
            final State currentState = getEngine().getCurrentState();

            //steps guidance
            try {
                this.guid.step(currentState);
            } catch (GuidanceException e) {
                throw new RuntimeException(e); //TODO better exception!
            }
            
            //manages jumps
            if (currentState.phase() != Phase.PRE_INITIAL && this.atJump) {
                try {
                    this.coverage.add(currentState.getCurrentMethodSignature().toString() + ":" + this.jumpPC + ":" + currentState.getCurrentProgramCounter());
                } catch (ThreadStackEmptyException e) {
                    //this should never happen
                    throw new RuntimeException(e); //TODO better exception!
                }
            }

            //manages string literals (they might be useful to EvoSuite)
            if (currentState.phase() != Phase.PRE_INITIAL && this.atLoadConstant) {
                try {
                    if (this.loadConstantStackSize == currentState.getStackSize()) {
                        final Value operand = currentState.getCurrentFrame().operands(1)[0];
                        if (operand instanceof Reference) {
                            final Reference r = (Reference) operand;
                            final Objekt o = currentState.getObject(r);
                            if (o != null && JAVA_STRING.equals(o.getType().getClassName())) {
                                final String s = valueString(currentState, r);
                                final long heapPosition = (r instanceof ReferenceConcrete ? ((ReferenceConcrete) r).getHeapPosition() : currentState.getResolution((ReferenceSymbolic) r));
                                this.stringLiterals.put(heapPosition, s);
                            }
                        }					
                    }
                } catch (FrozenStateException | InvalidNumberOfOperandsException | ThreadStackEmptyException e) {
                    throw new RuntimeException(e); //TODO better exception!
                }
            }

            return super.atStepPost();
        }

        @Override
        public boolean atPathEnd() {
            if (this.testDepth < 0) { //running a test up to the end
                return true;
            } else {
                return super.atPathEnd();
            }
        }
    }

    /**
     * The {@link Actions} for the {@link Runner} that runs the guided symbolic execution
     * up to the pre-frontier for a given depth.
     * 
     * @author Pietro Braione
     *
     */
    private static class ActionsRunnerPostFrontier extends Actions {
        private final HashMap<Long, String> stringLiterals;
        private final ArrayList<State> stateList = new ArrayList<>();
        private int testDepth;
        private boolean atJump = false;
        private int jumpPC = 0;
        private boolean atLoadConstant = false;
        private int loadConstantStackSize = 0;
        private final ArrayList<String> branchList = new ArrayList<>();

        public ActionsRunnerPostFrontier(HashMap<Long, String> stringLiterals) { 
            this.stringLiterals = stringLiterals;
        }
        
        public void setTestDepth(int testDepth) {
            this.testDepth = testDepth;
        }

        @Override
        public boolean atStepPre() {
            final State currentState = getEngine().getCurrentState();
            if (currentState.phase() != Phase.PRE_INITIAL) {
                try {
                    final byte currentInstruction = currentState.getInstruction();

                    //if at a jump bytecode, saves the start program counter
                    this.atJump = bytecodeJump(currentInstruction);
                    if (this.atJump) {
                        this.jumpPC = currentState.getCurrentProgramCounter();
                    }

                    //if at a load constant bytecode, saves the stack size
                    this.atLoadConstant = bytecodeLoadConstant(currentInstruction);
                    if (this.atLoadConstant) {
                        this.loadConstantStackSize = currentState.getStackSize();
                    }
                } catch (ThreadStackEmptyException | FrozenStateException e) {
                    //this should never happen
                    throw new RuntimeException(e); //TODO better exception!
                }
            }
            return super.atStepPre();
        }

        @Override
        public boolean atStepPost() {
            final State currentState = getEngine().getCurrentState();

            //manages string literals (they might be useful to EvoSuite)
            if (currentState.phase() != Phase.PRE_INITIAL && this.atLoadConstant) {
                try {
                    if (this.loadConstantStackSize == currentState.getStackSize()) {
                        final Value operand = currentState.getCurrentFrame().operands(1)[0];
                        if (operand instanceof Reference) {
                            final Reference r = (Reference) operand;
                            final Objekt o = currentState.getObject(r);
                            if (o != null && JAVA_STRING.equals(o.getType().getClassName())) {
                                final String s = valueString(currentState, r);
                                final long heapPosition = (r instanceof ReferenceConcrete ? ((ReferenceConcrete) r).getHeapPosition() : currentState.getResolution((ReferenceSymbolic) r));
                                this.stringLiterals.put(heapPosition, s);
                            }
                        }                                       
                    }
                } catch (FrozenStateException | InvalidNumberOfOperandsException | ThreadStackEmptyException e) {
                    throw new RuntimeException(e); //TODO better exception!
                }
            }

            if (currentState.getDepth() == this.testDepth + 1) {
                //we are at the first post-frontier state
                this.stateList.add(currentState.clone());
                if (this.atJump) {
                    try {
                        this.branchList.add(currentState.getCurrentMethodSignature().toString() + ":" + this.jumpPC + ":" + currentState.getCurrentProgramCounter());
                    } catch (ThreadStackEmptyException e) {
                        //this should never happen
                        throw new RuntimeException(e); //TODO better exception!
                    }
                } //else, do nothing
                getEngine().stopCurrentPath();
            }
            
            return super.atStepPost();
        }

        @Override
        public boolean atBacktrackPost(BranchPoint bp) {
            final State currentState = getEngine().getCurrentState();
            if (currentState.getDepth() == this.testDepth + 1) {
                //we are at another post-frontier state
                this.stateList.add(currentState.clone());
                if (this.atJump) {
                    try {
                        this.branchList.add(currentState.getCurrentMethodSignature().toString() + ":" + this.jumpPC + ":" + currentState.getCurrentProgramCounter());
                    } catch (ThreadStackEmptyException e) {
                        //this should never happen
                        throw new RuntimeException(e); //TODO better exception!
                    }
                } //else, do nothing
                getEngine().stopCurrentPath();            
                return super.atBacktrackPost(bp);
            } else {
                //we are at a lesser depth than testDepth + 1: no
                //more post-frontier states
                return true;
            }
        }
    }

    /**
     * Performs symbolic execution of the target method guided by a test case,
     * and returns the final state. Equivalent to {@link #runProgram(int) runProgram}{@code (-1).}
     * {@link List#get(int) get}{@code (0)}.
     *
     * @return the final {@link State}.
     * @throws DecisionException
     * @throws CannotBuildEngineException
     * @throws InitializationException
     * @throws InvalidClassFileFactoryClassException
     * @throws NonexistingObservedVariablesException
     * @throws ClasspathException
     * @throws CannotBacktrackException
     * @throws CannotManageStateException
     * @throws ThreadStackEmptyException
     * @throws ContradictionException
     * @throws EngineStuckException
     * @throws FailureException
     */
    public State runProgram()
    throws DecisionException, CannotBuildEngineException, InitializationException, 
    InvalidClassFileFactoryClassException, NonexistingObservedVariablesException, 
    ClasspathException, CannotBacktrackException, CannotManageStateException, 
    ThreadStackEmptyException, ContradictionException, EngineStuckException, 
    FailureException {
        return runProgram(-1).get(0); //depth -1 == never stop guidance
    }
    
    private Runner runnerPreFrontier = null;
    private ActionsRunnerPreFrontier actionsRunnerPreFrontier = null;
    private Runner runnerPostFrontier = null;
    private ActionsRunnerPostFrontier actionsRunnerPostFrontier = null;
    private State preState = null;
    
    /**
     * Performs symbolic execution of the target method guided by a test case 
     * up to some depth, then peeks the states on the next branch.  
     * 
     * @param testDepth the maximum depth up to which {@code t} guides 
     *        symbolic execution, or a negative value.
     * @return a {@link List}{@code <}{@link State}{@code >} containing
     *         all the states on branch at depth {@code stateDepth + 1}. 
     *         In case {@code stateDepth < 0} executes the test up to the 
     *         final state and returns a list containing only the final state.
     * @throws DecisionException
     * @throws CannotBuildEngineException
     * @throws InitializationException
     * @throws InvalidClassFileFactoryClassException
     * @throws NonexistingObservedVariablesException
     * @throws ClasspathException
     * @throws CannotBacktrackException
     * @throws CannotManageStateException
     * @throws ThreadStackEmptyException
     * @throws ContradictionException
     * @throws EngineStuckException
     * @throws FailureException
     */
    public List<State> runProgram(int testDepth)
    throws DecisionException, CannotBuildEngineException, InitializationException, 
    InvalidClassFileFactoryClassException, NonexistingObservedVariablesException, 
    ClasspathException, CannotBacktrackException, CannotManageStateException, 
    ThreadStackEmptyException, ContradictionException, EngineStuckException, 
    FailureException {
        //runs up to the pre-frontier
        if (this.runnerPreFrontier == null || this.runnerPreFrontier.getEngine().getCurrentState().getDepth() > testDepth) {
            makeRunnerPreFrontier();
        }
        this.actionsRunnerPreFrontier.setTestDepth(testDepth);
        this.runnerPreFrontier.run();
        
        if (testDepth < 0) {
            //there is no frontier, and the runnerPreFrontier's final
            //state is the final state of the guided execution: return it
            final State finalState = this.runnerPreFrontier.getEngine().getCurrentState();
            final ArrayList<State> retVal = new ArrayList<>();
            retVal.add(finalState.clone());
            return retVal;
        } else {
            //steps to all the post-frontier states and gathers them
            makeRunnerPostFrontier();
            if (this.preState.isStuck()) {
                return new ArrayList<>();
            } else {
                this.actionsRunnerPostFrontier.setTestDepth(testDepth);
                this.runnerPostFrontier.run();
                return this.actionsRunnerPostFrontier.stateList;
            }
        }
    }
    
    private void makeRunnerPreFrontier() throws DecisionException, NotYetImplementedException, 
    CannotBuildEngineException, InitializationException, InvalidClassFileFactoryClassException, 
    NonexistingObservedVariablesException, ClasspathException, ContradictionException {
        //builds the parameters
        final RunnerParameters pGuided = this.commonParamsGuided.clone();
        final RunnerParameters pGuiding = this.commonParamsGuiding.clone();
        completeParameters(pGuided, pGuiding);
        
        //builds the actions
        this.actionsRunnerPreFrontier = new ActionsRunnerPreFrontier((DecisionProcedureGuidance) pGuided.getDecisionProcedure());
        pGuided.setActions(this.actionsRunnerPreFrontier);

        //builds the runner
        final RunnerBuilder rb = new RunnerBuilder();
        this.runnerPreFrontier = rb.build(pGuided);
    }
    
    private void makeRunnerPostFrontier() throws DecisionException, NotYetImplementedException, 
    CannotBuildEngineException, InitializationException, InvalidClassFileFactoryClassException, 
    NonexistingObservedVariablesException, ClasspathException, ContradictionException {
        //builds the parameters
        final RunnerParameters pSymbolic = this.commonParamsGuided.clone();
        completeParametersSymbolic(pSymbolic);
        
        //gets the pre-frontier state and sets it as the initial
        //state of the post-frontier runner
        this.preState = this.runnerPreFrontier.getEngine().getCurrentState().clone();
        if (this.preState.isStuck()) {
            //degenerate case: execution ended before or at the pre-frontier
            this.runnerPostFrontier = null;
        } else { 
            pSymbolic.setStartingState(this.preState);

            //builds the actions
            this.actionsRunnerPostFrontier = new ActionsRunnerPostFrontier(this.actionsRunnerPreFrontier.stringLiterals);
            pSymbolic.setActions(this.actionsRunnerPostFrontier);

            //builds the runner
            final RunnerBuilder rb = new RunnerBuilder();
            this.runnerPostFrontier = rb.build(pSymbolic);
        }
    }

    private void completeParametersConcrete(RunnerParameters pConcrete) throws DecisionException {
        completeParameters(null, pConcrete);
    }

    private void completeParametersSymbolic(RunnerParameters pSymbolic) throws DecisionException {
        completeParameters(pSymbolic, null);
    }

    private void completeParameters(RunnerParameters pGuided, RunnerParameters pGuiding) throws DecisionException {
        //sets the calculator
        final CalculatorRewriting calc = new CalculatorRewriting();
        calc.addRewriter(new RewriterOperationOnSimplex());
        if (pGuiding == null) {
            //nothing
        } else {
            pGuiding.setCalculator(calc);
        }
        if (pGuided == null) {
            //nothing
        } else {
            pGuided.setCalculator(calc);
        }

        //sets the decision procedures
        final ArrayList<String> z3CommandLine = new ArrayList<>();
        z3CommandLine.add(this.z3Path);
        z3CommandLine.add(SWITCH_CHAR + "smt2");
        z3CommandLine.add(SWITCH_CHAR + "in");
        z3CommandLine.add(SWITCH_CHAR + "t:10");
        final ClassInitRulesRepo initRules = new ClassInitRulesRepo();
        try {
            if (pGuiding == null) {
                //nothing
            } else {
                final DecisionProcedureAlgorithms decGuiding = 
                    new DecisionProcedureAlgorithms(
                        new DecisionProcedureClassInit(
                            new DecisionProcedureAlwSat(calc), initRules));
                pGuiding.setDecisionProcedure(decGuiding);
            }
            if (pGuided == null) {
                //nothing
            } else {
                final DecisionProcedureAlgorithms decAlgo = 
                    new DecisionProcedureAlgorithms(
                        new DecisionProcedureClassInit(
                            new DecisionProcedureLICS( //useless?
                                new DecisionProcedureSMTLIB2_AUFNIRA(
                                    new DecisionProcedureAlwSat(calc), z3CommandLine), 
                                new LICSRulesRepo()), initRules)); 
                if (pGuiding == null) {
                    pGuided.setDecisionProcedure(decAlgo);
                } else {
                    final Signature stopSignature = (pGuided.getMethodSignature() == null ? pGuided.getStartingState().getRootMethodSignature() : pGuided.getMethodSignature());
                    final DecisionProcedureGuidanceJDI decGuided = 
                        new DecisionProcedureGuidanceJDI(decAlgo, calc, pGuiding, stopSignature, this.numberOfHits);
                    pGuided.setDecisionProcedure(decGuided);
                }
            }
        } catch (InvalidInputException | ThreadStackEmptyException e) {
            //this should never happen
            throw new AssertionError(e);
        }
    }

    private int countNumberOfInvocations(String methodClassName, String methodDescriptor, String methodName)
    throws CannotBuildEngineException, DecisionException, InitializationException, InvalidClassFileFactoryClassException, 
    NonexistingObservedVariablesException, ClasspathException, ContradictionException, CannotBacktrackException, 
    CannotManageStateException, ThreadStackEmptyException, EngineStuckException, FailureException {
        final RunnerParameters pConcrete = this.commonParamsGuiding.clone();
        completeParametersConcrete(pConcrete);        
        return countNonRecursiveHits(pConcrete, new Signature(methodClassName, methodDescriptor, methodName));
    }

    /**
     * Must be invoked after an invocation of {@link #runProgram(int) runProgram(depth)}.
     * Returns the initial state of symbolic execution.
     * 
     * @return a {@link State} or {@code null} if this method is invoked
     *         before an invocation of {@link #runProgram(int)}.
     */
    public State getInitialState() {
        State retVal = this.commonParamsGuided.getStartingState();
        if (retVal == null) {
            retVal = this.runnerPreFrontier.getEngine().getInitialState();
            this.commonParamsGuided.setStartingState(retVal);
        }
        return retVal;
    }

    /**
     * Must be invoked after an invocation of {@link #runProgram(int) runProgram(depth)}.
     * Returns the state of symbolic execution at depth {@code depth}.
     * 
     * @return a {@link State} or {@code null} if this method is invoked
     *         before an invocation of {@link #runProgram(int)}.
     */
    public State getPreState() {
        return this.preState;
    }

    /**
     * Must be invoked after an invocation of {@link #runProgram(int) runProgram(depth)}.
     * Returns whether the frontier is at a jump bytecode.
     * 
     * @return a {@code boolean}. If this method is invoked
     *         before an invocation of {@link #runProgram(int)}
     *         always returns {@code false}.
     */
    public boolean getAtJump() {
        return this.actionsRunnerPostFrontier.atJump;
    }

    /**
     * Must be invoked after an invocation of {@link #runProgram(int) runProgram(depth)}.
     * Returns the branches that the states at depth {@code depth + 1} cover.
     * 
     * @return a {@link List}{@code <}{@link String}{@code >} if {@link #getAtJump() getAtJump}{@code () == true}. 
     *         In this case {@code getTargetBranches().}{@link List#get(int) get}{@code (i)} is the branch
     *         covered by the {@code i}-th state in the list returned by {@link #runProgram(int) runProgram(depth)}.
     *         If  {@link #getAtJump() getAtJump}{@code () == false}, or if the method is
     *         invoked before an invocation of {@link #runProgram(int)}, returns {@code null}.
     */
    public List<String> getTargetBranches() {
        return this.actionsRunnerPostFrontier.branchList;
    }

    /**
     * Must be invoked after an invocation of {@link #runProgram(TestCase, int) runProgram(tc, depth)}.
     * Returns the string literals of the execution.
     * 
     * @return a {@link Map}{@code <}{@link Long}{@code , }{@link String}{@code >}, 
     *         mapping a heap position of a {@link String} literal to the
     *         corresponding value of the literal. 
     *         If this method is invoked
     *         before an invocation of {@link #runProgram(TestCase, int)}
     *         always returns {@code null}.
     */
    public Map<Long, String> getStringLiterals() {
        return this.actionsRunnerPostFrontier.stringLiterals;
    }

    /**
     * Must be invoked after an invocation of {@link #runProgram(TestCase, int) runProgram(tc, depth)}.
     * Returns the code branches covered by the execution.
     * 
     * @return a {@link HashSet}{@code <}{@link String}{@code >} where each {@link String} has the form
     *         className:methodDescriptor:methodName:bytecodeFrom:bytecodeTo, or {@code null} if this method is invoked
     *         before an invocation of {@link #runProgram(TestCase, int)}.
     */
    public HashSet<String> getCoverage() {
        return this.actionsRunnerPreFrontier.coverage;
    }

    @Override
    public void close() throws DecisionException {
        if (this.runnerPreFrontier != null) {
            this.runnerPreFrontier.getEngine().close();
        }
    }
}
