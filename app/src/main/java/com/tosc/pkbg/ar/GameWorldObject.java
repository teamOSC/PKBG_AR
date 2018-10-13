package com.tosc.pkbg.ar;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import java.util.ArrayList;
import java.util.Arrays;

public class GameWorldObject {

    public ArrayList<Float> position;
    public ArrayList<Float> rotation;
    public String id;

    public GameWorldObject() {

    }

    public GameWorldObject(Vector3 pos, Quaternion rot, String id) {
        Float[] positionArray = new Float[] {pos.x, pos.y, pos.z};
        this.position = new ArrayList<>(Arrays.asList(positionArray));
        Float[] rotationArray = new Float[] {rot.w, rot.x, rot.y, rot.z};
        this.rotation = new ArrayList<>(Arrays.asList(rotationArray));
        this.id = id;
    }
}
