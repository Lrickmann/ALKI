import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import de.learnlib.algorithms.lstar.dfa.ClassicLStarDFA;
import de.learnlib.algorithms.lstar.dfa.ClassicLStarDFABuilder;
import de.learnlib.api.oracle.MembershipOracle.DFAMembershipOracle;
import de.learnlib.filter.statistic.oracle.DFACounterOracle;
import de.learnlib.oracle.equivalence.DFAWMethodEQOracle;
import de.learnlib.oracle.membership.SimulatorOracle.DFASimulatorOracle;
import de.learnlib.util.Experiment.DFAExperiment;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import net.automatalib.util.automata.random.RandomAutomata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class uebung_08 {
    private static HashMap<Integer, ArrayList<Integer>> countMemberQueries = new HashMap<Integer, ArrayList<Integer>>();
    private static HashMap<Integer, ArrayList<Integer>> countEquivQueries = new HashMap<Integer, ArrayList<Integer>>();
    private static final RandomAutomata randAutomata = RandomAutomata.getInstance();
    private static final Alphabet<Character> sigma = Alphabets.characters('0', '1');

    private static CompactDFA<Character> get_dfa(int n) {
        return randAutomata.randomDFA(n, sigma);
    }

    public static void main(String[] args) throws IOException, PythonExecutionException {
// load DFA and alphabet
        for (int n = 1; n <= 100; n++) {
            countMemberQueries.put(n, new ArrayList<>());
            countEquivQueries.put(n, new ArrayList<>());

            for (int i = 0; i < 100; i++) {
                CompactDFA<Character> target = get_dfa(n);

                // construct a simulator membership query oracle
                // input  - Character (determined by example)
                DFAMembershipOracle<Character> membershipOracle = new DFASimulatorOracle<>(target);
                // DFAEquivalenceOracle<Character> equivalenceOracle = new DFASimulatorOracle<>(target);
                // oracle for counting queries wraps SUL
                DFACounterOracle<Character> mqOracle = new DFACounterOracle<>(membershipOracle, "membership queries");

                // construct L* instance
                ClassicLStarDFA<Character> lstar =
                        new ClassicLStarDFABuilder<Character>().withAlphabet(sigma) // input alphabet
                                .withOracle(mqOracle) // membership oracle
                                .create();
                // construct a W-method conformance test
                // exploring the system up to depth 4 from
                // every state of a hypothesis
                DFAWMethodEQOracle<Character> wMethod = new DFAWMethodEQOracle<>(mqOracle, 4);

                // construct a learning experiment from
                // the learning algorithm and the conformance test.
                // The experiment will execute the main loop of
                // active learning
                DFAExperiment<Character> experiment = new DFAExperiment<>(lstar, wMethod, sigma);

                // turn on time profiling
                experiment.setProfile(true);

                // enable logging of models
                experiment.setLogModels(true);

                // run experiment
                experiment.run();

                // add membership count to hashmap
                countMemberQueries.get(n).add((int) mqOracle.getStatisticalData().getCount());
                // add equivalence count to hashmap
                countEquivQueries.get(n).add((int) experiment.getRounds().getCount());

            }
        }
        ArrayList<Double> mbLine = new ArrayList<>();
        ArrayList<Double> eqLine = new ArrayList<>();

        for(int i = 1; i<= 100;i++){
            // calculate mean per n
            mbLine.add(countMemberQueries.get(i).stream().mapToInt(Integer::intValue).summaryStatistics().getAverage());
            eqLine.add(countEquivQueries.get(i).stream().mapToInt(Integer::intValue).summaryStatistics().getAverage());
        }
        // plot membership mean per n
        Plot plt = Plot.create();
        plt.plot().add(mbLine).label("membership");
        plt.legend().loc("upper right");
        plt.title("L*");
        plt.show();
        // plot equivalence mean per n
        Plot plt2 = Plot.create();
        plt2.plot().add(eqLine).label("equivalence");
        plt2.legend().loc("upper right");
        plt2.title("L*");
        plt2.show();
    }
}
