/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.simpleEditor;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.debug.WireBox;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mifth
 */
public class EditorSelectionManager extends AbstractControl {

    private AssetManager assetMan;
    private Node root, guiNode;
    private Application app;
    private EditorBaseManager base;
    private static List<Long> selectionList;
    private Transform selectionCenter;
    private SelectionToolType selectionToolType;
    private EditorSelectionTools selectionTools;
    private boolean isActive;
    private SelectionMode selectionMode;
    private long lastSelected;

    protected enum SelectionToolType {

        All, MouseClick, Rectangle, Polygon
    };

    protected enum SelectionMode {

        Normal, Additive, Substractive
    };

    public EditorSelectionManager(Application app, EditorBaseManager base) {

        this.app = app;
        this.base = base;
        assetMan = this.app.getAssetManager();
        root = (Node) this.app.getViewPort().getScenes().get(0);
        guiNode = (Node) this.app.getGuiViewPort().getScenes().get(0);

        isActive = false;
        selectionCenter = null;
        selectionList = new ArrayList<Long>();

        selectionTools = new EditorSelectionTools(this.app, this.base, this);
        selectionToolType = SelectionToolType.MouseClick;
        selectionMode = selectionMode.Normal;


    }

    protected boolean activate() {
        boolean result = false;

        if (selectionToolType == SelectionToolType.MouseClick) {
            selectionTools.selectMouseClick();
//            base.getGuiManager().setSelectedObjectsList();
            result = true;
        } else if (selectionToolType == SelectionToolType.Rectangle) {
//            selectionTools.drawRectangle();
            isActive = true;
            result = true;
        }

        return result;

    }

    protected void deactivate() {

        // SELECT ENTITIES OF THE RECTANGLE TOOL
        if (selectionToolType == SelectionToolType.Rectangle && isActive) {
            selectEntities();
            selectionTools.clearRectangle();
            System.out.println("deact");
        }

        isActive = false;
        calculateSelectionCenter();

        // SET HISTORY
        System.out.println("selHistory");
        base.getHistoryManager().setNewSelectionHistory(selectionList);


    }

    protected void selectEntity(long ID, SelectionMode mode) {
        Node nodeToSelect = (Node) base.getSpatialSystem().getSpatialControl(ID).getGeneralNode();

        if (mode == SelectionMode.Normal) {
            // remove selection boxes
            for (Long idToRemove : selectionList) {
                removeSelectionBox((Node) base.getSpatialSystem().getSpatialControl(idToRemove).getGeneralNode());
            }
            selectionList.clear();

            // add to selection
            selectionList.add(ID);
            createSelectionBox(nodeToSelect);

        } else if (mode == SelectionMode.Additive) {
            if (selectionList.contains(ID)) {
                selectionList.remove(ID);
                removeSelectionBox(nodeToSelect); // remove selection mesh
            } else {
                selectionList.add(ID);
                createSelectionBox(nodeToSelect);
            }
        }
        // Substractive is not implemented        

//        base.getGuiManager().setSelectedObjectsList();
    }

    protected void selectEntities() {

        List<Node> lst = base.getLayerManager().getLayers();
        Vector2f centerCam = new Vector2f(app.getCamera().getWidth() * 0.5f, app.getCamera().getHeight() * 0.5f);
        Node rectangle = selectionTools.getRectangleSelection();
        Vector3f rectanglePosition = rectangle.getLocalTranslation();

        if (selectionMode == SelectionMode.Normal) {
            // remove selection boxes
            for (Long idToRemove : selectionList) {
                removeSelectionBox((Node) base.getSpatialSystem().getSpatialControl(idToRemove).getGeneralNode());
            }

            // clear selectionList
            selectionList.clear();
        }

        for (Node layer : lst) {
            // check if layer is enabled
            Object boolObj = layer.getUserData("isEnabled");
            boolean bool = (Boolean) boolObj;

            if (bool == true) {
                for (Spatial sp : layer.getChildren()) {

                    Vector3f spScreenPos = app.getCamera().getScreenCoordinates(sp.getWorldTranslation());
                    float spScreenDistance = centerCam.distance(new Vector2f(spScreenPos.getX(), spScreenPos.getY()));

                    if (spScreenPos.getZ() < 1f) {

                        float pointMinX = Math.min(rectanglePosition.getX(), spScreenPos.getX());
                        float pointMaxX = Math.max(rectanglePosition.getX(), spScreenPos.getX());
                        float pointMinY = Math.min(rectanglePosition.getY(), spScreenPos.getY());
                        float pointMaxY = Math.max(rectanglePosition.getY(), spScreenPos.getY());

                        float distX = pointMaxX - pointMinX;
                        float distY = pointMaxY - pointMinY;

                        //add to selection the spatial which is in the rectangle area
                        if (distX <= rectangle.getLocalScale().getX() * 0.5f
                                && distY <= rectangle.getLocalScale().getY() * 0.5f) {
                            Object spIdObj = sp.getUserData("EntityID");
                            long spId = (Long) spIdObj;
                            if (selectionMode == SelectionMode.Additive) {
                                selectEntity(spId, selectionMode);
                            } else if (selectionMode == SelectionMode.Normal) {
                                selectionList.add(spId);
                                Node nodeToSelect = (Node) base.getSpatialSystem().getSpatialControl(spId).getGeneralNode();
                                createSelectionBox(nodeToSelect);
                            }
                        }
                    }
                }
            }
        }
        // select item in the objectslist        
        if (selectionMode == SelectionMode.Normal) {
//            base.getGuiManager().setSelectedObjectsList();
        }
    }

    protected void createSelectionBox(Node nodeSelect) {
        Material mat_box = new Material(assetMan, "Common/MatDefs/Misc/Unshaded.j3md");
        mat_box.setColor("Color", new ColorRGBA(0.5f, 0.3f, 0.1f, 1));
        WireBox wbx = new WireBox();
//        BoundingBox = new BoundingBox();
        Transform tempScale = nodeSelect.getLocalTransform().clone();
        nodeSelect.setLocalTransform(new Transform());
        wbx.fromBoundingBox((BoundingBox) nodeSelect.getWorldBound());
        nodeSelect.setLocalTransform(tempScale);

        Geometry bx = new Geometry("SelectionTempMesh", wbx);
        bx.setMaterial(mat_box);
        nodeSelect.attachChild(bx);

    }

    protected void removeSelectionBox(Node nodeSelect) {
        nodeSelect.detachChild(nodeSelect.getChild("SelectionTempMesh"));
    }

    protected void clearSelectionList() {
        for (Long id : selectionList) {
            removeSelectionBox((Node) base.getSpatialSystem().getSpatialControl(id).getGeneralNode());
        }
        selectionList.clear();
    }

    protected Transform getSelectionCenter() {
        return selectionCenter;
    }

    protected void setSelectionCenter(Transform selectionTransform) {
        this.selectionCenter = selectionTransform;
    }

    protected boolean isIsActive() {
        return isActive;
    }

    protected void calculateSelectionCenter() {
        if (selectionList.size() == 0) {
            selectionCenter = null;
        } else if (selectionList.size() == 1) {
            Spatial nd = base.getSpatialSystem().getSpatialControl(selectionList.get(0)).getGeneralNode();
            selectionCenter = nd.getLocalTransform().clone();
        } else if (selectionList.size() > 1) {

            if (selectionCenter == null) {
                selectionCenter = new Transform();
            }

            // FIND CENTROID OF center POSITION
            Vector3f centerPosition = new Vector3f();
            for (Long ID : selectionList) {
//                // POSITION
                Spatial ndPos = base.getSpatialSystem().getSpatialControl(ID).getGeneralNode();
                centerPosition.addLocal(ndPos.getWorldTranslation());
            }
            centerPosition.divideLocal(selectionList.size());
            selectionCenter.setTranslation(centerPosition);

            // Rotation of the last selected is Local Rotation (like in Blender)
            Quaternion rot = base.getSpatialSystem().getSpatialControl(selectionList.get(selectionList.size() - 1)).getGeneralNode().getLocalRotation();
//                TransformComponent trLastSelected = (TransformComponent) base.getEntityManager().getComponent(selectionList.get(selectionList.size() - 1), TransformComponent.class);
            selectionCenter.setRotation(rot); //Local coordinates of the last object            
        }

        //set the last selected color
        if (selectionList.size() > 0) {
            // set for previous selected
            if (selectionList.size() > 1 && selectionList.contains(lastSelected)) {
                Node ndPrevious = (Node) base.getSpatialSystem().getSpatialControl(lastSelected).getGeneralNode();
                Geometry geoBoxPrevious = (Geometry) ndPrevious.getChild("SelectionTempMesh");
                geoBoxPrevious.getMaterial().setColor("Color", new ColorRGBA(0.5f, 0.3f, 0.1f, 1));
            }

            // set for new selected
            long lastID = selectionList.get(selectionList.size() - 1);
            Node ndLast = (Node) base.getSpatialSystem().getSpatialControl(lastID).getGeneralNode();
            Geometry geoBoxLast = (Geometry) ndLast.getChild("SelectionTempMesh");
            geoBoxLast.getMaterial().setColor("Color", new ColorRGBA(0.8f, 0.6f, 0.2f, 1));
            lastSelected = lastID;
        }

    }

    protected List<Long> getSelectionList() {
        return selectionList;
    }

    public SelectionToolType getSelectionTool() {
        return selectionToolType;
    }

    public void setSelectionTool(SelectionToolType selectionTool) {
        this.selectionToolType = selectionTool;
    }

    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(SelectionMode selectionMode) {
        this.selectionMode = selectionMode;
    }

    @Override
    protected void controlUpdate(float tpf) {

        if (isActive) {
            selectionTools.drawRectangle();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
