package com.example.bwm.MCTS;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

// 扩展游戏状态接口，增加启发式评估方法
interface GameState {
    List<GameState> getNextStates();
    boolean isTerminal();
    double evaluate();
    void switchPlayer();
    GameState copy();

    // 新增：评估当前状态的启发值（用于模拟阶段的启发式选择）
    double getHeuristicValue();

    // 新增：获取当前玩家
    int getCurrentPlayer();
}

// 蒙特卡洛树节点
class Node {
    private GameState state;
    private Node parent;
    private List<Node> children;
    private int visits;
    private double score;

    public Node(GameState state) {
        this.state = state;
        this.children = new ArrayList<>();
        this.visits = 0;
        this.score = 0;
        this.parent = null;
    }

    public Node(GameState state, Node parent) {
        this(state);
        this.parent = parent;
    }

    public void addChild(Node child) {
        children.add(child);
    }

    public void update(double result) {
        visits++;
        score += result;
    }

    // getter和setter方法
    public GameState getState() { return state; }
    public Node getParent() { return parent; }
    public List<Node> getChildren() { return children; }
    public int getVisits() { return visits; }
    public double getScore() { return score; }
    public boolean hasChildren() { return !children.isEmpty(); }
}

// 优化的蒙特卡洛树搜索算法，带有启发式模拟策略
public class MCTS {
    private static final int SIMULATION_DEPTH = 100;
    private static final double EXPLORATION_PARAM = Math.sqrt(2);
    // 模拟策略参数
    private static final double EPSILON = 0.1; // 探索概率，10%概率随机选择
    private static final int HEURISTIC_DEPTH = 3; // 启发式评估的前瞻深度

    public GameState search(GameState initialState, int iterations) {
        Node root = new Node(initialState);

        for (int i = 0; i < iterations; i++) {
            Node selectedNode = select(root);
            Node expandedNode = expand(selectedNode);
            double result = simulate(expandedNode.getState());
            backpropagate(expandedNode, result);
        }

        return getBestChild(root, 0).getState();
    }

    private Node select(Node node) {
        while (!node.getState().isTerminal()) {
            if (node.getChildren().size() < node.getState().getNextStates().size()) {
                return node;
            } else {
                node = getBestChild(node, EXPLORATION_PARAM);
            }
        }
        return node;
    }

    private Node expand(Node node) {
        GameState state = node.getState();
        List<GameState> nextStates = state.getNextStates();

        Set<GameState> existingStates = new HashSet<>();
        for (Node child : node.getChildren()) {
            existingStates.add(child.getState());
        }

        for (GameState nextState : nextStates) {
            if (!existingStates.contains(nextState)) {
                Node newNode = new Node(nextState, node);
                node.addChild(newNode);
                return newNode;
            }
        }

        return getBestChild(node, 0); // 返回当前最佳子节点
    }

    // 优化的模拟策略：结合启发式评估和随机探索
    private double simulate(GameState state) {
        GameState tempState = state.copy();
        int depth = 0;

        while (!tempState.isTerminal() && depth < SIMULATION_DEPTH) {
            List<GameState> nextStates = tempState.getNextStates();
            if (nextStates.isEmpty()) break;

            GameState nextState;
            // 以一定概率进行随机探索，避免陷入局部最优
            if (ThreadLocalRandom.current().nextDouble() < EPSILON) {
                // 随机选择
                int randomIndex = ThreadLocalRandom.current().nextInt(nextStates.size());
                nextState = nextStates.get(randomIndex);
            } else {
                // 基于启发式评估选择最佳下一步
                nextState = selectBestNextState(nextStates);
            }

            tempState = nextState;
            depth++;
        }

        return tempState.evaluate();
    }

    // 基于启发式评估选择最佳下一步
    private GameState selectBestNextState(List<GameState> nextStates) {
        double bestValue = Double.NEGATIVE_INFINITY;
        GameState bestState = null;
        int currentPlayer = nextStates.get(0).getCurrentPlayer();

        // 为每个可能的下一步状态计算启发值
        for (GameState state : nextStates) {
            // 计算多步前瞻的启发值
            double value = evaluateHeuristic(state, HEURISTIC_DEPTH);

            // 根据当前玩家调整价值评估（最大化自己的优势）
            if (currentPlayer == 1) {
                if (value > bestValue) {
                    bestValue = value;
                    bestState = state;
                }
            } else {
                if (value < bestValue) {
                    bestValue = value;
                    bestState = state;
                }
            }
        }

        return bestState != null ? bestState : nextStates.get(0);
    }

    // 多步前瞻的启发式评估
    private double evaluateHeuristic(GameState state, int depth) {
        if (depth == 0 || state.isTerminal()) {
            return state.getHeuristicValue();
        }

        List<GameState> nextStates = state.getNextStates();
        if (nextStates.isEmpty()) {
            return state.getHeuristicValue();
        }

        double bestValue;
        int currentPlayer = state.getCurrentPlayer();

        // 递归评估下一步状态
        if (currentPlayer == 1) {
            bestValue = Double.NEGATIVE_INFINITY;
            for (GameState next : nextStates) {
                double value = evaluateHeuristic(next, depth - 1);
                bestValue = Math.max(bestValue, value);
            }
        } else {
            bestValue = Double.POSITIVE_INFINITY;
            for (GameState next : nextStates) {
                double value = evaluateHeuristic(next, depth - 1);
                bestValue = Math.min(bestValue, value);
            }
        }

        // 结合当前状态的启发值和未来状态的评估
        return 0.7 * bestValue + 0.3 * state.getHeuristicValue();
    }

    private void backpropagate(Node node, double result) {
        while (node != null) {
            node.update(result);
            result = -result; // 切换玩家视角
            node = node.getParent();
        }
    }

    private Node getBestChild(Node node, double explorationParam) {
        Node bestChild = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Node child : node.getChildren()) {
            if (child.getVisits() == 0) {
                return child; // 优先选择未访问过的节点
            }

            double exploit = child.getScore() / child.getVisits();
            double explore = Math.sqrt(Math.log(node.getVisits()) / child.getVisits());
            double score = exploit + explorationParam * explore;

            if (score > bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }

        return bestChild != null ? bestChild : node.getChildren().get(0);
    }
}
