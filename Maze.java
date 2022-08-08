import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

//represents a class that allows edges to be compared
class CompareVal implements Comparator<Edge> {
  // compares vals of two edges
  public int compare(Edge e1, Edge e2) {
    return e1.val - e2.val;
  }

}

//represents an edge
class Edge {
  Cell reference;
  Cell connecter;
  int val;

  Edge(Cell reference, Cell connecter) {
    this.reference = reference;
    this.connecter = connecter;
    Random r = new Random();
    int random = r.nextInt(10000);
    this.val = random;
  }

}

//represents a cell
class Cell {
  int x;
  int y;
  Color color;
  Cell top;
  Cell left;
  Cell right;
  Cell bottom;

  Cell(int x, int y) {
    this.x = x;
    this.y = y;
    this.color = Color.gray;
    this.top = null;
    this.left = null;
    this.right = null;
    this.bottom = null;
  }

}

//represents a maze
class Maze extends World {
  int x;
  int y;
  int cellSize;
  int wrongMoves;
  ArrayList<Cell> cells;
  HashMap<Cell, Cell> disjoint;
  ArrayList<Edge> edges;
  ArrayList<Edge> removedEdges;
  WorldScene scene;
  boolean bfs;
  boolean dfs;
  Stack<Cell> workList;
  Deque<Cell> workList2;
  ArrayList<Cell> seen;
  HashMap<Cell, Cell> cameFromEdge;
  Cell key;

  Maze(int x, int y, int cellSize) {
    this.x = x;
    this.y = y;
    this.cellSize = cellSize;
    wrongMoves = 0;
    cells = new ArrayList<Cell>();
    disjoint = new HashMap<Cell, Cell>();
    edges = new ArrayList<Edge>();
    removedEdges = new ArrayList<Edge>();
    scene = new WorldScene(x * cellSize, y * cellSize);
    bfs = false;
    dfs = false;

    generateCells();
    linkCells();
    generateEdges();
    generateDisjoint();
    makeMaze();

    for (int i = 0; i < cells.size(); i++) {
      drawCell(cells.get(i));
    }

    seen = new ArrayList<Cell>();
    workList = new Stack<Cell>();
    workList.push(cells.get(0));
    workList2 = new ArrayDeque<Cell>();
    workList2.addFirst(cells.get(0));
    cameFromEdge = new HashMap<Cell, Cell>();

    key = cells.get(x * y - 1);
  }

  // constructor for tests only
  Maze(int x, int y, boolean testIndicator) {
    this.x = x;
    this.y = y;
    this.cellSize = 20;
    cells = new ArrayList<Cell>();
    disjoint = new HashMap<Cell, Cell>();
    edges = new ArrayList<Edge>();
    removedEdges = new ArrayList<Edge>();
    scene = new WorldScene(x * 20, y * 20);
    bfs = false;
    dfs = false;
  }

  // draws a cell onto the scene
  // EEFECT: mutates the scene by adding a cell image onto it
  void drawCell(Cell c) {

    if (c.x == 0 && c.y == 0) {
      if (c.color != Color.blue) {
        c.color = Color.green;
      }
      WorldImage cell = new RectangleImage(cellSize, cellSize, OutlineMode.SOLID, c.color);
      scene.placeImageXY(cell, c.x * cellSize + cellSize / 2, c.y * cellSize + cellSize / 2);
    }
    else if (c.x == this.x - 1 && c.y == this.y - 1) {
      if (c.color == Color.gray) {
        c.color = Color.pink;
      }
      WorldImage cell = new RectangleImage(cellSize, cellSize, OutlineMode.SOLID, c.color);
      scene.placeImageXY(cell, c.x * cellSize + cellSize / 2, c.y * cellSize + cellSize / 2);
    }
    else {
      WorldImage cell = new RectangleImage(cellSize, cellSize, OutlineMode.SOLID, c.color);
      scene.placeImageXY(cell, c.x * cellSize + cellSize / 2, c.y * cellSize + cellSize / 2);
    }
    if (c.right != null && (!edgeContain(c, c.right))) {
      WorldImage line = new RectangleImage(2, cellSize, OutlineMode.SOLID, Color.black);
      scene.placeImageXY(line, c.x * cellSize + cellSize, c.y * cellSize + cellSize / 2);
    }
    if (c.bottom != null && (!edgeContain(c, c.bottom))) {
      WorldImage line = new RectangleImage(cellSize, 2, OutlineMode.SOLID, Color.black);
      scene.placeImageXY(line, c.x * cellSize + cellSize / 2, c.y * cellSize + cellSize);
    }
  }

  // generates all cells in the maze
  // EFFECT: adds cells to the cells array list
  void generateCells() {
    for (int i = 0; i < y; i++) {
      for (int j = 0; j < x; j++) {
        cells.add(new Cell(j, i));
      }
    }
  }

  // Links all the cells to each other to make connected grid
  // EFFECT: Links all cells in cells
  void linkCells() {
    for (int i = 1; i < x * y; i++) {
      if (i % x == 0) {
        cells.get(i - 1).right = null;
        cells.get(i).left = null;
      }
      else {
        cells.get(i - 1).right = cells.get(i);
        cells.get(i).left = cells.get(i - 1);
      }
      if (i < x * y - (x - 1)) {
        cells.get(i - 1).bottom = cells.get(i + (x - 1));
        cells.get(i + (x - 1)).top = cells.get(i - 1);
      }
      else {
        cells.get(i - 1).bottom = null;
      }
    }
  }

  // Generates the list of edge objects that represents edge between two cells
  // EFFECT: adds edges to the edges array list
  void generateEdges() {
    for (int i = 0; i < cells.size(); i++) {
      if (cells.get(i).right != null) {
        Edge edge = new Edge(cells.get(i), cells.get(i).right);
        edges.add(edge);
      }
      if (cells.get(i).bottom != null) {
        Edge edge = new Edge(cells.get(i), cells.get(i).bottom);
        edges.add(edge);
      }
    }
    edges.sort(new CompareVal());
  }

  // Generates the starting Hash map
  // EFFECT: Generates the starting disjoint set represented by a Hash map
  void generateDisjoint() {
    for (int i = 0; i < cells.size(); i++) {
      disjoint.put(cells.get(i), cells.get(i));
    }
  }

  // Creates a complete maze by using Kruskal's algorithm to remove edges using a
  // MST
  // EFFECT: Generates a complete maze by mutating the hash map
  void makeMaze() {
    for (int i = 0; i < edges.size(); i++) {
      if (find(edges.get(i).reference) != find(edges.get(i).connecter)) {
        union(find(edges.get(i).reference), find(edges.get(i).connecter));
        removedEdges.add(edges.get(i));
      }
    }
  }

  // returns the key in the hash map of the given cell as the value
  Cell find(Cell c) {
    if (disjoint.get(c).equals(c)) {
      return c;
    }
    else {
      return find(disjoint.get(c));
    }
  }

  // changes a cells reference point
  // EEFECT: changes a cells key in the hash map
  void union(Cell c1, Cell c2) {
    disjoint.put(c1, c2);
  }

  // makes the scene
  public WorldScene makeScene() {
    return scene;
  }

  // changes the scene based on the key pressed
  // EEFECT: tells the program to reset, run dfs, or bfs
  public void onKeyEvent(String k) {
    if (k.equals("b")) {
      this.dfs = false;
      this.bfs = true;
    }
    if (k.equals("d")) {
      this.bfs = false;
      this.dfs = true;
    }
    if (k.equals("r")) {
      wrongMoves = 0;
      cells = new ArrayList<Cell>();
      disjoint = new HashMap<Cell, Cell>();
      edges = new ArrayList<Edge>();
      removedEdges = new ArrayList<Edge>();
      scene = new WorldScene(x * cellSize, y * cellSize);
      bfs = false;
      dfs = false;

      generateCells();
      linkCells();
      generateEdges();
      generateDisjoint();
      makeMaze();

      for (int i = 0; i < cells.size(); i++) {
        drawCell(cells.get(i));
      }

      seen = new ArrayList<Cell>();
      workList = new Stack<Cell>();
      workList.push(cells.get(0));
      workList2 = new ArrayDeque<Cell>();
      workList2.addFirst(cells.get(0));
      cameFromEdge = new HashMap<Cell, Cell>();

      key = cells.get(x * y - 1);
    }
  }

  // checks for changes every tick
  // EEFECT: runs either bfs or dfs if the boolean is true
  public void onTick() {

    if (bfs) {
      if (workList2.size() > 0) {
        bfs();
      }
      else {
        bestPath();
      }
    }
    if (dfs) {
      if (workList.size() > 0) {
        dfs();
      }
      else {
        bestPath();
      }
    }

  }

  // runs a breadth first search to complete the maze
  // EFFECT: mutates the scene to show the steps of the breadth first search
  void bfs() {
    Cell c = workList2.removeFirst();

    int xColor = 255 / this.x;
    int yColor = 255 / this.y;
    c.color = new Color(c.x * xColor, 255, c.y * yColor);
    drawCell(c);
    wrongMoves += 1;
    if (c == cells.get((x * y) - 1)) {
      workList2 = new ArrayDeque<Cell>();
    }
    if (!seen.contains(c)) {
      {
        seen.add(c);
        if (c.right != null && !seen.contains(c.right)) {
          if (edgeContain(c, c.right)) {
            cameFromEdge.put(c.right, c);
            workList2.addLast(c.right);
          }
        }
        if (c.bottom != null && !seen.contains(c.bottom)) {
          if (edgeContain(c, c.bottom)) {
            cameFromEdge.put(c.bottom, c);
            workList2.addLast(c.bottom);
          }
        }
        if (c.top != null && !seen.contains(c.top)) {
          if (edgeContain(c.top, c)) {
            cameFromEdge.put(c.top, c);
            workList2.addLast(c.top);
          }
        }
        if (c.left != null && !seen.contains(c.left)) {
          if (edgeContain(c.left, c)) {
            cameFromEdge.put(c.left, c);
            workList2.addLast(c.left);
          }
        }
      }
    }
  }

  // runs a depth first search to complete the maze
  // EFFECT: mutates the scene to show the steps of the depth first search
  void dfs() {
    Cell c = workList.pop();

    int xColor = 255 / this.x;
    int yColor = 255 / this.y;
    c.color = new Color(c.x * xColor, 255, c.y * yColor);
    drawCell(c);
    wrongMoves += 1;
    if (c == cells.get((x * y) - 1)) {
      workList = new Stack<Cell>();
    }
    if (!seen.contains(c)) {
      seen.add(c);
      if (c.top != null && !seen.contains(c.top)) {
        if (edgeContain(c.top, c)) {
          cameFromEdge.put(c.top, c);
          workList.push(c.top);

        }
      }
      if (c.left != null && !seen.contains(c.left)) {
        if (edgeContain(c.left, c)) {
          cameFromEdge.put(c.left, c);
          workList.push(c.left);

        }
      }
      if (c.right != null && !seen.contains(c.right)) {
        if (edgeContain(c, c.right)) {
          cameFromEdge.put(c.right, c);
          workList.push(c.right);

        }
      }
      if (c.bottom != null && !seen.contains(c.bottom)) {
        if (edgeContain(c, c.bottom)) {
          cameFromEdge.put(c.bottom, c);
          workList.push(c.bottom);

        }
      }
    }
  }

  // checks to see if the list of removed edges contains two specific cells
  boolean edgeContain(Cell to, Cell from) {
    for (int i = 0; i < removedEdges.size(); i++) {
      if (removedEdges.get(i).reference.x == to.x && removedEdges.get(i).reference.y == to.y
          && removedEdges.get(i).connecter.x == from.x
          && removedEdges.get(i).connecter.y == from.y) {
        return true;
      }
    }
    return false;
  }

  // solves the maze
  // EFFECT: mutates the scene to show the steps of solving the maze
  void bestPath() {
    if (key != null) {
      Cell map = cameFromEdge.get(key);
      key.color = Color.blue;
      drawCell(key);
      key = map;
      wrongMoves -= 1;
      redrawEdges();
    }
    else {
      scene.placeImageXY(new TextImage(this.wrongMoves + "  wrong moves!", 15, Color.black),
          x * cellSize / 2, y * cellSize / 2);
      scene.placeImageXY(new TextImage("'r' to reset!", 15, Color.black), x * cellSize / 2,
          y * cellSize / 2 + 15);
    }

  }

  // re draws the maze line to make the maze more visable
  // EFFECT: mutates the scene to re add lines onto it
  void redrawEdges() {
    for (int i = 0; i < edges.size(); i++) {
      if (!removedEdges.contains(edges.get(i))) {
        if (edges.get(i).reference.x == edges.get(i).connecter.x) {
          WorldImage line = new RectangleImage(cellSize, 2, OutlineMode.SOLID, Color.black);
          scene.placeImageXY(line, edges.get(i).reference.x * cellSize + cellSize / 2,
              edges.get(i).reference.y * cellSize + cellSize);
        }
        if (edges.get(i).reference.y == edges.get(i).connecter.y) {
          WorldImage line = new RectangleImage(2, cellSize, OutlineMode.SOLID, Color.black);
          scene.placeImageXY(line, edges.get(i).reference.x * cellSize + cellSize,
              edges.get(i).reference.y * cellSize + cellSize / 2);
        }
      }
    }
  }
}

//Examples and tests
class ExamplesMaze {
  // displays the game
  void testGame(Tester t) {
    Maze m = new Maze(20, 20, 30);
    m.bigBang(m.x * m.cellSize, m.y * m.cellSize, .005);
  }

  // Tester for compare
  void testComparator(Tester t) {
    // Example cells
    Cell cell1 = new Cell(0, 0);
    Cell cell2 = new Cell(1, 0);
    Cell cell3 = new Cell(0, 2);
    Cell cell4 = new Cell(1, 2);
    Cell cell5 = new Cell(0, 5);
    Cell cell6 = new Cell(1, 5);
    Cell cell7 = new Cell(0, 7);
    Cell cell8 = new Cell(1, 7);
    Cell cell9 = new Cell(0, 10);
    Cell cell10 = new Cell(1, 10);
    Cell cell11 = new Cell(0, 14);
    Cell cell12 = new Cell(1, 14);
    CompareVal c = new CompareVal();
    // example edges
    Edge edge1 = new Edge(cell1, cell2);
    Edge edge2 = new Edge(cell3, cell4);
    Edge edge3 = new Edge(cell5, cell6);
    Edge edge4 = new Edge(cell7, cell8);
    Edge edge5 = new Edge(cell9, cell10);
    Edge edge6 = new Edge(cell11, cell12);
    edge1.val = 1;
    edge2.val = 3;
    edge3.val = 10;
    edge4.val = 5;
    edge5.val = 20;
    edge6.val = 100;

    t.checkExpect(c.compare(edge1, edge2), -2);
    t.checkExpect(c.compare(edge3, edge4), 5);
    t.checkExpect(c.compare(edge5, edge6), -80);

  }

  // Tester for generateCells
  void testGenerateCells(Tester t) {
    Maze m = new Maze(5, 5, 20);
    ArrayList<Cell> test = new ArrayList<Cell>();
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 5; j++) {
        test.add(new Cell(j, i));
      }
    }
    for (int i = 1; i < 25; i++) {
      if (i % 5 == 0) {
        test.get(i - 1).right = null;
        test.get(i).left = null;
      }
      else {
        test.get(i - 1).right = test.get(i);
        test.get(i).left = test.get(i - 1);
      }
      if (i < 21) {
        test.get(i - 1).bottom = test.get(i + 4);
        test.get(i + 4).top = test.get(i - 1);
      }
      else {
        test.get(i - 1).bottom = null;
      }
    }
    test.get(0).color = Color.green;
    test.get(24).color = Color.pink;
    t.checkExpect(m.cells, test);

    Maze m2 = new Maze(10, 10, 20);
    ArrayList<Cell> test2 = new ArrayList<Cell>();
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        test2.add(new Cell(j, i));
      }
    }
    for (int i = 1; i < 100; i++) {
      if (i % 10 == 0) {
        test2.get(i - 1).right = null;
        test2.get(i).left = null;
      }
      else {
        test2.get(i - 1).right = test2.get(i);
        test2.get(i).left = test2.get(i - 1);
      }
      if (i < 91) {
        test2.get(i - 1).bottom = test2.get(i + 9);
        test2.get(i + 9).top = test2.get(i - 1);
      }
      else {
        test2.get(i - 1).bottom = null;
      }
    }
    test2.get(0).color = Color.green;
    test2.get(99).color = Color.pink;
    t.checkExpect(m2.cells, test2);
  }

  // Tester for linkCells
  void testLinkCells(Tester t) {
    Maze m = new Maze(5, 5, 20);
    // top left cell
    t.checkExpect(m.cells.get(0).right, m.cells.get(1));
    t.checkExpect(m.cells.get(1).left, m.cells.get(0));
    t.checkExpect(m.cells.get(0).bottom, m.cells.get(5));
    t.checkExpect(m.cells.get(5).top, m.cells.get(0));
    t.checkExpect(m.cells.get(0).left, null);
    t.checkExpect(m.cells.get(0).top, null);

    // bottom right cell
    t.checkExpect(m.cells.get(24).left, m.cells.get(23));
    t.checkExpect(m.cells.get(23).right, m.cells.get(24));
    t.checkExpect(m.cells.get(19).bottom, m.cells.get(24));
    t.checkExpect(m.cells.get(24).top, m.cells.get(19));
    t.checkExpect(m.cells.get(24).right, null);
    t.checkExpect(m.cells.get(24).bottom, null);

    // middle cell
    t.checkExpect(m.cells.get(12).right, m.cells.get(13));
    t.checkExpect(m.cells.get(13).left, m.cells.get(12));
    t.checkExpect(m.cells.get(12).top, m.cells.get(7));
    t.checkExpect(m.cells.get(7).bottom, m.cells.get(12));
    t.checkExpect(m.cells.get(12).bottom, m.cells.get(17));
    t.checkExpect(m.cells.get(17).top, m.cells.get(12));
    t.checkExpect(m.cells.get(12).left, m.cells.get(11));
    t.checkExpect(m.cells.get(11).right, m.cells.get(12));

  }

  // Tester for generateEdges
  void testGenerateEdges(Tester t) {
    Maze m = new Maze(5, 5, true);
    m.generateCells();
    m.linkCells();
    t.checkExpect(m.edges, new ArrayList<Edge>());
    m.generateEdges();
    t.checkExpect(m.edges.size(), 40);
    Maze m1 = new Maze(10, 10, true);
    m1.generateCells();
    m1.linkCells();
    t.checkExpect(m1.edges, new ArrayList<Edge>());
    m1.generateEdges();
    t.checkExpect(m1.edges.size(), 180);
  }

  // Tester for generateDisjoint
  void testGenerateDisjoint(Tester t) {
    Maze m = new Maze(5, 5, true);
    m.generateCells();
    m.linkCells();
    m.generateEdges();
    t.checkExpect(m.disjoint, new HashMap<Cell, Cell>());
    m.generateDisjoint();
    for (int i = 0; i < m.cells.size(); i++) {
      t.checkExpect(m.find(m.cells.get(i)), m.cells.get(i));
    }
  }

  // Tester for makeMaze
  void testMakeMaze(Tester t) {
    Maze m = new Maze(5, 5, true);
    m.generateCells();
    m.linkCells();
    m.generateEdges();
    m.generateDisjoint();
    t.checkExpect(m.find(m.cells.get(0)), m.find(m.cells.get(0)));
    t.checkExpect(m.find(m.cells.get(10)), m.find(m.cells.get(10)));
    m.makeMaze();
    t.checkExpect(m.find(m.cells.get(0)), m.find(m.cells.get(0)));
    t.checkExpect(m.find(m.cells.get(10)), m.find(m.cells.get(0)));
  }

  // Tester for find
  void testFind(Tester t) {
    Maze m1 = new Maze(5, 5, 20);
    Maze m2 = new Maze(5, 5, true);
    m2.generateCells();
    m2.linkCells();
    m2.generateEdges();
    m2.generateDisjoint();
    t.checkExpect(m1.find(m1.cells.get(10)), m1.find(m1.cells.get(0)));
    t.checkExpect(m2.find(m2.cells.get(10)), m2.find(m2.cells.get(10)));
  }

  // Tester for union
  void testUnion(Tester t) {
    Maze m = new Maze(5, 5, true);
    m.generateCells();
    m.linkCells();
    m.generateEdges();
    m.generateDisjoint();
    t.checkExpect(m.find(m.cells.get(0)), m.find(m.cells.get(0)));
    t.checkExpect(m.find(m.cells.get(1)), m.find(m.cells.get(1)));
    m.union(m.find(m.cells.get(0)), m.find(m.cells.get(1)));
    t.checkExpect(m.find(m.cells.get(0)), m.find(m.cells.get(0)));
    t.checkExpect(m.find(m.cells.get(1)), m.find(m.cells.get(0)));
    t.checkExpect(m.find(m.cells.get(2)), m.find(m.cells.get(2)));
    t.checkExpect(m.find(m.cells.get(3)), m.find(m.cells.get(3)));
    m.union(m.find(m.cells.get(2)), m.find(m.cells.get(3)));
    t.checkExpect(m.find(m.cells.get(2)), m.find(m.cells.get(2)));
    t.checkExpect(m.find(m.cells.get(3)), m.find(m.cells.get(2)));
    m.union(m.find(m.cells.get(1)), m.find(m.cells.get(2)));
    t.checkExpect(m.find(m.cells.get(2)), m.find(m.cells.get(1)));
    t.checkExpect(m.find(m.cells.get(3)), m.find(m.cells.get(0)));
  }

  // tester for drawCell
  void testDrawCell(Tester t) {
    Maze m = new Maze(5, 5, true);
    WorldScene sceneTest = new WorldScene(m.x * m.cellSize, m.y * m.cellSize);
    Cell c1 = new Cell(0, 0);
    Cell c2 = new Cell(1, 1);
    Cell c3 = new Cell(4, 4);
    Cell c4 = new Cell(1, 0);
    Cell c5 = new Cell(0, 1);
    c1.right = c4;
    c1.bottom = c5;
    m.drawCell(c1);
    WorldImage cell1 = new RectangleImage(m.cellSize, m.cellSize, OutlineMode.SOLID, Color.green);
    sceneTest.placeImageXY(cell1, c1.x * m.cellSize + m.cellSize / 2,
        c1.y * m.cellSize + m.cellSize / 2);
    WorldImage line1 = new RectangleImage(2, m.cellSize, OutlineMode.SOLID, Color.black);
    sceneTest.placeImageXY(line1, c1.x * m.cellSize + m.cellSize,
        c1.y * m.cellSize + m.cellSize / 2);
    WorldImage line2 = new RectangleImage(m.cellSize, 2, OutlineMode.SOLID, Color.black);
    sceneTest.placeImageXY(line2, c1.x * m.cellSize + m.cellSize / 2,
        c1.y * m.cellSize + m.cellSize);
    t.checkExpect(m.scene, sceneTest);
    m.drawCell(c2);
    WorldImage cell2 = new RectangleImage(m.cellSize, m.cellSize, OutlineMode.SOLID, Color.gray);
    sceneTest.placeImageXY(cell2, c2.x * m.cellSize + m.cellSize / 2,
        c2.y * m.cellSize + m.cellSize / 2);
    t.checkExpect(m.scene, sceneTest);
    m.drawCell(c3);
    WorldImage cell3 = new RectangleImage(m.cellSize, m.cellSize, OutlineMode.SOLID, Color.pink);
    sceneTest.placeImageXY(cell3, c3.x * m.cellSize + m.cellSize / 2,
        c3.y * m.cellSize + m.cellSize / 2);
    t.checkExpect(m.scene, sceneTest);
  }

  // Tester for onKeyEvent
  void testOnKeyEvent(Tester t) {
    Maze m1 = new Maze(5, 5, 20);
    m1.onKeyEvent("b");
    t.checkExpect(m1.bfs, true);
    t.checkExpect(m1.dfs, false);
    Maze m2 = new Maze(5, 5, 20);
    m2.onKeyEvent("d");
    t.checkExpect(m2.bfs, false);
    t.checkExpect(m2.dfs, true);
    m2.wrongMoves = 1;
    t.checkExpect(m2.wrongMoves, 1);
    m2.onKeyEvent("r");
    t.checkExpect(m2.bfs, false);
    t.checkExpect(m2.dfs, false);
    t.checkExpect(m2.wrongMoves, 0);
  }

  // tester for onTick()
  void testOnTick(Tester t) {
    Maze m1 = new Maze(5, 5, 20);
    t.checkExpect(m1.wrongMoves, 0);
    m1.onKeyEvent("b");
    m1.onTick();
    t.checkExpect(m1.wrongMoves, 1);
    Maze m2 = new Maze(5, 5, 20);
    t.checkExpect(m2.wrongMoves, 0);
    m2.onKeyEvent("d");
    m2.onTick();
    t.checkExpect(m2.wrongMoves, 1);
  }

  // tester for bfs
  void testBFS(Tester t) {
    Maze m = new Maze(5, 5, 20);
    ArrayList<Cell> test = new ArrayList<Cell>();
    t.checkExpect(m.workList2.getFirst(), m.cells.get(0));
    t.checkExpect(m.wrongMoves, 0);
    t.checkExpect(m.seen, new ArrayList<Cell>());
    m.bfs();
    test.add(m.cells.get(0));
    t.checkExpect(m.wrongMoves, 1);
    t.checkExpect(m.seen, test);
    if (m.edgeContain(m.cells.get(0), m.cells.get(0).right)) {
      t.checkExpect(m.workList2.getFirst(), m.cells.get(0).right);
      m.bfs();
      test.add(m.cells.get(0).right);
      t.checkExpect(m.wrongMoves, 2);
      t.checkExpect(m.seen, test);
      t.checkExpect(m.cells.get(0).right.color,
          new Color(m.cells.get(0).right.x * 255 / m.x, 255, m.cells.get(0).right.y * 255 / m.y));
    }
    else if (m.edgeContain(m.cells.get(0), m.cells.get(0).bottom)) {
      t.checkExpect(m.workList2.getFirst(), m.cells.get(0).bottom);
      test.add(m.cells.get(0).bottom);
      m.bfs();
      t.checkExpect(m.wrongMoves, 2);
      t.checkExpect(m.seen, test);
      t.checkExpect(m.cells.get(0).bottom.color,
          new Color(m.cells.get(0).bottom.x * 255 / m.x, 255, m.cells.get(0).bottom.y * 255 / m.y));
      if (m.edgeContain(m.cells.get(0).bottom, m.cells.get(0).bottom.right)) {
        t.checkExpect(m.workList2.getFirst(), m.cells.get(0).bottom.right);
      }
      else if (m.edgeContain(m.cells.get(0).bottom, m.cells.get(0).bottom.bottom)) {
        t.checkExpect(m.workList2.getFirst(), m.cells.get(0).bottom.bottom);
      }
    }

  }

  // tester for dfs
  void testDFS(Tester t) {
    Maze m = new Maze(5, 5, 20);
    ArrayList<Cell> test = new ArrayList<Cell>();
    t.checkExpect(m.workList.firstElement(), m.cells.get(0));
    t.checkExpect(m.wrongMoves, 0);
    t.checkExpect(m.seen, new ArrayList<Cell>());
    m.dfs();
    test.add(m.cells.get(0));
    t.checkExpect(m.wrongMoves, 1);
    t.checkExpect(m.seen, test);
    if (m.edgeContain(m.cells.get(0), m.cells.get(0).right)) {
      t.checkExpect(m.workList.firstElement(), m.cells.get(0).right);
      m.dfs();
      test.add(m.cells.get(0).right);
      t.checkExpect(m.wrongMoves, 2);
      t.checkExpect(m.seen, test);
      if (m.edgeContain(m.cells.get(0).right, m.cells.get(0).right.right)) {
        t.checkExpect(m.workList.firstElement(), m.cells.get(0).right.right);

      }
      else if (m.edgeContain(m.cells.get(0).right, m.cells.get(0).right.bottom)) {
        t.checkExpect(m.workList.firstElement(), m.cells.get(0).right.bottom);
      }

    }
    else if (m.edgeContain(m.cells.get(0), m.cells.get(0).bottom)) {
      t.checkExpect(m.workList.firstElement(), m.cells.get(0).bottom);
      m.dfs();
      test.add(m.cells.get(0).bottom);
      t.checkExpect(m.wrongMoves, 2);
      t.checkExpect(m.seen, test);
      if (m.edgeContain(m.cells.get(0).bottom, m.cells.get(0).bottom.right)) {
        t.checkExpect(m.workList.firstElement(), m.cells.get(0).bottom.right);

      }
      else if (m.edgeContain(m.cells.get(0).bottom, m.cells.get(0).bottom.bottom)) {
        t.checkExpect(m.workList.firstElement(), m.cells.get(0).bottom.bottom);
      }
    }

  }

  // tester for bestPath
  void testBestPath(Tester t) {
    Maze m = new Maze(5, 5, 20);
    m.dfs();
    m.dfs();
    t.checkExpect(m.wrongMoves, 2);
    t.checkExpect(m.cells.get(24).color, Color.pink);
    m.bestPath();
    t.checkExpect(m.wrongMoves, 1);
    t.checkExpect(m.cells.get(24).color, Color.blue);
  }

  // tester for edgeContain
  void testEdgeContain(Tester t) {
    Maze m = new Maze(5, 5, true);
    Cell c1 = new Cell(0, 0);
    Cell c2 = new Cell(0, 1);
    Cell c3 = new Cell(2, 2);
    Cell c4 = new Cell(3, 2);
    t.checkExpect(m.edgeContain(c1, c2), false);
    t.checkExpect(m.edgeContain(c3, c4), false);
    m.removedEdges.add(new Edge(c1, c2));
    t.checkExpect(m.edgeContain(c1, c2), true);
    t.checkExpect(m.edgeContain(c3, c4), false);
    m.removedEdges.add(new Edge(c3, c4));
    t.checkExpect(m.edgeContain(c1, c2), true);
    t.checkExpect(m.edgeContain(c3, c4), true);
  }

  // tester for redrawLines
  void testRedrawLines(Tester t) {
    Maze m = new Maze(5, 5, 20);
    WorldScene sceneTest = m.scene;
    m.redrawEdges();

    for (int i = 0; i < m.edges.size(); i++) {
      if (!m.removedEdges.contains(m.edges.get(i))) {
        if (m.edges.get(i).reference.x == m.edges.get(i).connecter.x) {
          WorldImage line = new RectangleImage(m.cellSize, 2, OutlineMode.SOLID, Color.black);
          sceneTest.placeImageXY(line, m.edges.get(i).reference.x * m.cellSize + m.cellSize / 2,
              m.edges.get(i).reference.y * m.cellSize + m.cellSize);
        }
        if (m.edges.get(i).reference.y == m.edges.get(i).connecter.y) {
          WorldImage line = new RectangleImage(2, m.cellSize, OutlineMode.SOLID, Color.black);
          sceneTest.placeImageXY(line, m.edges.get(i).reference.x * m.cellSize + m.cellSize,
              m.edges.get(i).reference.y * m.cellSize + m.cellSize / 2);
        }
      }
    }

    t.checkExpect(m.scene, sceneTest);
  }
  
  
}