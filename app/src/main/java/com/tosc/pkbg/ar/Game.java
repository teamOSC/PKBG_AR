package com.tosc.pkbg.ar;

import java.util.ArrayList;

public class Game {

  public String id;

  public GamePlayer player1;
  public GamePlayer player2;

  public Game() {

  }

  public Game(String id) {
    this.id = id;
  }

  public ArrayList<GameWorldObject> gameWorldObject;
}
