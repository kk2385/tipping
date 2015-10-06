import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Created by ylc265 on 9/23/15.
 */
public class DannaJack2 extends NoTippingPlayer{
    private Random strategy;
    private int player;
    private List<Integer> weights;
    private List<Weight> weights_on_board;
    private int[] board;
    private boolean firstRemove;

    DannaJack2(int port) {
        super(port);
    }

    protected String process(String command) {
        if (strategy == null) {
            // initialize
            strategy = new Random();
            // assume player is 2 (0,1) for (player1, player2) respectively
            this.player = 1;
            weights = new ArrayList<Integer>();
            for (int i = 1; i <= 15; i ++) {
                weights.add(i);
            }

            weights_on_board = new ArrayList<Weight>();
            // put the original 3 kg block on board
            weights_on_board.add(new Weight(3, -4, 1));
            board = new int[50];
            board[-4+25] = 3;
            firstRemove = false;
        }
        StringTokenizer tk = new StringTokenizer(command);

        // get the command, and opponent's position and weight last round.
        command = tk.nextToken();
        int position = Integer.parseInt(tk.nextToken());
        int weight = Integer.parseInt(tk.nextToken());

        // in the beginning of game, whoever gets 0, 0 for position and weight is
        // player 1
        if (position == 0 && weight == 0) {
            this.player = 0;
            firstRemove = true;
        } else {
            // execute previous player's move
            if (command.equals("ADDING")) {
                weights_on_board.add(new Weight(weight, position, (player+1)%2));
                board[position+25] = weight;
            } else {
                // The last user add will end up with the following message
                // REMOVING position weight
                // we must add the position and weight as player 1 before removing.
                if (weights_on_board.size() == 30 && firstRemove) {
                    weights_on_board.add(new Weight(weight, position, (player + 1) % 2));
                    firstRemove = false;
                } else {
                    removeWeight(weights_on_board, position, weight);
                }
            }
        }

        // make the moves
        Weight decision;
        if (command.equals("ADDING")) {
            decision = makeAddMove(weights, board, weights_on_board);
            // update board
            weights_on_board.add(decision);
        } else {
            decision = makeRemoveMove(weights_on_board);
            // update board
            weights_on_board.remove(decision);
        }
        return decision.position + " " + decision.weight;
    }

    public Weight makeAddMove(List<Integer> weights, int[] board, List<Weight> weights_on_board) {
    	//greedily select a move will generate the least torque.
    	int bestPos = 0;
    	int bestWeight = 0;
    	int smallestTorque = 1 << 20;
    	for (Integer weight : weights) {
    		for (int pos = -25; pos < 25; pos++) {
    			 if (board[pos+25] == 0 && validAddMove(weight, pos, weights_on_board)) {
    				 int dist1 = Math.abs(pos-(-1));
    				 int dist2 = Math.abs(pos-(-3));
    				 int closerSupport = (dist1 < dist2)? -1 : -3;
    				 int torque = weight*Math.abs(pos-closerSupport);
    				 if (torque < smallestTorque) {
    					 bestPos = pos;
    					 bestWeight = weight;
    					 smallestTorque = torque;
    				 }
    			 }
    		}
    	}
    	System.out.printf("Adding Best weight: %d best position: %d\n", bestWeight, bestPos);
    	board[bestPos+25] = bestWeight;
    	weights.remove(new Integer(bestWeight));
    	return new Weight(bestWeight, bestPos, player); //if none found will return (0,0), which leads to loss.
    }

    private boolean validAddMove(int weight, int position, List<Weight> weights_on_board) {
        List<Weight> temp = new ArrayList<Weight>();
        for (Weight w: weights_on_board) {
            temp.add(w);
        }
        temp.add(new Weight(weight, position, player));
        return verifyGameNotOver(temp);
    }

    public Weight makeRemoveMove(List<Weight> weights_on_board) {
        List<Weight> candidates = new ArrayList<Weight>();
        List<Weight> remove_candidate = new ArrayList<Weight>();
        // player 1 can remove anything
        if (player == 0) {
            remove_candidate = weights_on_board;
        } else {
            // player 2 can only remove his/her own piece unless there are none
            remove_candidate = getMyBlocks(weights_on_board, player);
            if (remove_candidate.size() == 0){
                // no more player 2 blocks
                remove_candidate = weights_on_board;
            }
        }
        for (Weight w: remove_candidate) {
            if (canRemove(w, weights_on_board)) {
                candidates.add(w);
            }
        }
        
        if (candidates.size() == 0) { //no candidates means we lost, just return first weight on board.
        	return weights_on_board.get(0);
        }
        
        
        
        //Strategy: Look Two steps ahead. (Simulate a move from me, then from opponent).
        Weight best = candidates.get(0);
        System.out.printf("%d Candidates, first one: %s\n", candidates.size(), best);
        int bestGoodness = 0; //a vague measurement of whether a block is a good candidate.
        for (Weight w : candidates) {
        	int currGoodness = 0;
        	int opponentTipTimes = 0; //number of weights that my opponent CANT remove after I remove w. (coz they'll lose)
        	List<Weight> simulation = copy(player == 0? candidates : weights_on_board); //simulate. player1 gets to remove everything, player2 only removes candidates.
        	simulation.remove(w);
        	for (Weight w1 : simulation) {
        		if (!canRemove(w1, simulation)) { //opponent is screwed.
        			opponentTipTimes++;
        			currGoodness++;
        		} else {
        			List<Weight> simulateAgain = copy(simulation); //simulate opponent removing w1.
        			simulateAgain.remove(w1);
        			for (Weight w2 : candidates) { 
        				if (canRemove(w2, simulateAgain)) {//confirmed won't be screwed.
        					currGoodness++;
        					break;
        				}
        			}
        		}
        	}
        	if (opponentTipTimes == weights_on_board.size()-1) { //making this move will guarantee win.
        		System.out.printf("	DESTRUCTION! Removing %s which guarantees victory.\n", w);
        		return w;
        	} else if (currGoodness > bestGoodness) {
        		best = w;
        		bestGoodness = currGoodness;
        		System.out.printf("	New best: %s with goodness %d\n", best, bestGoodness);
        	}
        }
        System.out.printf("Returning best: %s with goodness %d\n", best, bestGoodness);
        return best;
    }
    
    private List<Weight> copy(List<Weight> li) {
    	ArrayList<Weight> res = new ArrayList<Weight>();
    	for (Weight w : li) res.add(w);
    	return res;
    }

    private boolean canRemove(Weight weight, List<Weight> weights_on_board) {
        List<Weight> temp = new ArrayList<Weight>();
        for (Weight w: weights_on_board) {
            temp.add(w);
        }
        temp.remove(weight);
        return verifyGameNotOver(temp);
    }

    private List<Weight> getMyBlocks(List<Weight> weights_on_board, int player) {
        List<Weight> returnBlock = new ArrayList<Weight>();
        for (Weight w: weights_on_board) {
            if (w.player == player) {
                returnBlock.add(w);
            }
        }
        return returnBlock;
    }

    private void removeWeight(List<Weight> weights_on_board, int pos, int weight) {
        Weight retW = new Weight(0, 0, 0);
        for (Weight w : weights_on_board) {
            if (w.position == pos && w.weight == weight) {
               retW = w;
            }
        }
        weights_on_board.remove(retW);
    }

    private boolean verifyGameNotOver(List<Weight> weights_on_board) {
        int left_torque = 0;
        int right_torque = 0;
        for (Weight weight: weights_on_board) {
            left_torque -= (weight.position - (-3)) * weight.weight;
            right_torque -= (weight.position - (-1)) * weight.weight;
        }
        boolean gameOver = (left_torque > 0 || right_torque < 0);
        return !gameOver;
    }

    public static void main(String[] args) throws Exception {
        new DannaJack2(Integer.parseInt(args[0]));
    }

    public class Weight {
        public int weight;
        public int position;
        public int player;
        public Weight(int weight, int position, int player) {
            this.weight = weight;
            this.position = position;
            this.player = player;
        }
        public boolean equals(Weight other) {
        	return this.weight == other.weight &&
        			this.position == other.position; //does not track player.
        }
        
        public String toString() {
        	return String.format("%d %d", this.weight, this.position);
        }
    }

}
