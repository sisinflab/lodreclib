package it.poliba.sisinflab.LODRec.utils;

import java.util.ArrayList;
import java.util.List;

public class BST {
	private Node root;
	private int numNodes = 0;
	private int maxSize = 0;
	private List<Double> sortedKeys;
	private List<Integer> sortedValues;

	public BST(int max) {
		this.maxSize = max;
	}

	public int getNumNodes() {
		return this.numNodes;
	}

	private class Node {
		private double key;
		private int value;
		private boolean isRoot = false;
		private Node left, right;

		public Node(double k, int v) {
			this.key = k;
			this.value = v;
		}

		public Node(double k, int v, boolean isRoot) {
			this.key = k;
			this.value = v;
			this.isRoot = isRoot;
		}
	}


	public void visit() {

		sortedKeys = new ArrayList<Double>();
		sortedValues = new ArrayList<Integer>();

		if (root != null)
			visit(root);

	}

	private void visit(Node node) {

		if (node.right != null)
			visit(node.right);

		this.sortedKeys.add(node.key);
		this.sortedValues.add(node.value);

		if (node.left != null)
			visit(node.left);

	}

	public List<Integer> getSortedValues() {
		return this.sortedValues;
	}

	public List<Double> getSortedKeys() {
		return this.sortedKeys;
	}

	public void insert(double k, int v) {

		// System.out.println("inserting " + k);
		// this.numNodes++;
		if (root == null) {
			root = new Node(k, v, true);
			this.numNodes++;
		} else
			insert(root, k, v);

		// keep only the maxSize nodes with highest value
		if (this.numNodes > this.maxSize) {
			// System.out.println("---pruning----");
			// Node min = getMin();
			deleteMin(root);
			// delete min
		}

	}

	// public Node getMin() {
	// if (root == null)
	// return null;
	// else
	// return getMin(root);
	// }
	//
	// private Node getMin(Node node) {
	// if (node.left == null)
	// return node;
	// else
	// return getMin(node.left);
	// }

	private Node deleteMin(Node node) {

		// min node is root
		if (node.isRoot & node.left == null) {
			this.root = node.right;
			this.root.isRoot = true;
			this.numNodes--;
			return null;
		}

		// min node has no childs
		if (node.left == null & node.right == null) {
			// if true -> node is min
			this.numNodes--;
			return null;

			// min node has right child
		} else if (node.left == null & node.right != null) {
			this.numNodes--;
			return node.right;

		}

		Node ret = deleteMin(node.left);
		node.left = ret;

		return node;

	}

	public Node insert(Node curr, double k, int v) {

		if (k >= curr.key) {

			if (curr.right != null)
				return insert(curr.right, k, v);
			else {
				curr.right = new Node(k, v);
				this.numNodes++;
			}

		} else {

			if (curr.left != null)
				return insert(curr.left, k, v);
			else {
				curr.left = new Node(k, v);
				this.numNodes++;
			}
		}

		return null;

	}

	public static void main(String[] args) {

		BST bst = new BST(10);

		bst.insert(6.0, 2);
		bst.insert(3.0, 3);
		bst.insert(1.0, 5);

		bst.insert(5.0, 5);
		bst.insert(7.0, 3);
		bst.insert(6.0, 2);
		bst.insert(8.0, 3);
		bst.insert(3.0, 3);
		bst.insert(4.0, 5);
		bst.insert(0.0, 3);
		bst.insert(1.0, 2);
		bst.insert(10.0, 2);
		bst.insert(1.0, 2);
		bst.insert(100.0, 2);
		bst.insert(15.0, 2);
		bst.insert(110.0, 2);
		bst.insert(5.0, 5);
		bst.insert(7.0, 3);
		bst.insert(6.0, 2);
		bst.insert(8.0, 3);
		bst.insert(3.0, 3);
		bst.insert(4.0, 5);
		bst.insert(0.0, 3);
		bst.insert(1.0, 2);
		bst.insert(5.0, 5);
		bst.insert(7.0, 3);
		bst.insert(6.0, 2);
		bst.insert(8.0, 3);
		bst.insert(3.0, 3);
		bst.insert(4.0, 5);
		bst.insert(0.0, 3);
		bst.insert(1.0, 2);
		bst.insert(10.0, 2);
		bst.insert(100.0, 2);
		bst.insert(15.0, 2);
		bst.insert(110.0, 2);

		bst.insert(3.0, 3);
		bst.insert(4.0, 5);
		bst.insert(0.0, 3);
		bst.insert(1.0, 2);
		bst.insert(10.0, 2);
		bst.insert(100.0, 2);
		bst.insert(15.0, 2);
		
		System.out.println("final tree");
		System.out.println("num nodes " + bst.numNodes);
		bst.visit();
		System.out.println("size: " + bst.sortedKeys.size());
		for (double i : bst.sortedKeys) {
			System.out.println(i);
		}
		// System.out.println(bst.getMin().key);

		// bst.descVisit();
		// bst.deleteMin(bst.root);
	}

}
