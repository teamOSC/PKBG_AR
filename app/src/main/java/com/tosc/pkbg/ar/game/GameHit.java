package com.tosc.pkbg.ar.game;

public class GameHit {
    public static final int HIT_HEAD = 0;
    public static final int HIT_BODY = 1;

    public String hitBy;
    public int hitType;


    public GameHit() {

    }

    public GameHit(String hitBy, int hitType) {
        this.hitBy = hitBy;
        this.hitType = hitType;
    }
}
