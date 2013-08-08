/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entitysystem;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

/**
 *
 * @author mifth
 */
public class EntityTransformComponent {

    private Transform transform;
    private Vector3f location, scale;
    private Quaternion rotation;

    public EntityTransformComponent(Transform trans) {
        this.transform = trans;
        this.location = trans.getTranslation();
        this.rotation = trans.getRotation();
        this.scale = trans.getScale();
    }

    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    public void setLocation(Vector3f location) {
        this.location = location;
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
    }

    public void setRotation(Quaternion rotation) {
        this.rotation = rotation;
    }

    public Transform getTransform() {
        return transform;
    }
    
    public Vector3f getLocation() {
        return location;
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public Vector3f getScale() {
        return scale;
    }    

}