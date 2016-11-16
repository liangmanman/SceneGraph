package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import util.IVertexData;
import util.Light;
import util.PolygonMesh;
import util.TextureImage;

import java.util.*;

/**
 * A specific implementation of this scene graph. This implementation is still independent
 * of the rendering technology (i.e. OpenGL)
 * @author Amit Shesh
 */
public class Scenegraph<VertexType extends IVertexData> implements IScenegraph<VertexType>
{
    /**
     * The root of the scene graph tree
     */
    protected INode root;

    /**
     * A map to store the (name,mesh) pairs. A map is chosen for efficient search
     */
    protected Map<String,util.PolygonMesh<VertexType>> meshes;

    /**
     * A map to store the (name,node) pairs. A map is chosen for efficient search
     */
    protected Map<String,INode> nodes;

    protected Map<String,String> textures;

    /**
     * The associated renderer for this scene graph. This must be set before attempting to
     * render the scene graph
     */
    protected IScenegraphRenderer renderer;

    int time = 0;
    int legTime = 0;
    boolean addTime, moveUp;
    List<Light> allLights;
    int angleOfDegree;

    public Scenegraph()
    {
        root = null;
        meshes = new HashMap<String,util.PolygonMesh<VertexType>>();
        nodes = new HashMap<String,INode>();
        time = 0;
        addTime = true;
        moveUp = true;
        allLights = new ArrayList<Light>();
        this.textures = new HashMap<>();
        angleOfDegree = 0;
    }

    public void dispose()
    {
        renderer.dispose();
    }

    /**
     * Sets the renderer, and then adds all the meshes to the renderer.
     * This function must be called when the scene graph is complete, otherwise not all of its
     * meshes will be known to the renderer
     * @param renderer The {@link IScenegraphRenderer} object that will act as its renderer
     * @throws Exception
     */
    @Override
    public void setRenderer(IScenegraphRenderer renderer) throws Exception {
        this.renderer = renderer;


      List<Light> allLightsDescend = new ArrayList<>();
        this.getRoot().descendLights(allLightsDescend, new Matrix4f());


      renderer.getAllLight(allLightsDescend);

        //now add all the meshes
        for (String meshName:meshes.keySet())
        {
            this.renderer.addMesh(meshName,meshes.get(meshName));
        }
        for (String texName : textures.keySet()) {
            TextureImage textureImage;

            textureImage = new TextureImage(textures.get(texName), "png",
              texName);

            this.renderer.addTexture(texName,textureImage);
        }

    }

    /**
     * Set the root of the scenegraph, and then pass a reference to this scene graph object
     * to all its node. This will enable any node to call functions of its associated scene graph
     * @param root
     */

    @Override
    public void makeScenegraph(INode root)
    {
        this.root = root;
        this.root.setScenegraph(this);

    }

    /**
     * Draw this scene graph. It delegates this operation to the renderer
     * @param modelView
     */
    @Override
    public void draw(Stack<Matrix4f> modelView) {
        if ((root!=null) && (renderer!=null))
        {
            if (moveUp) {
                time++;
            }
            else {
                time--;
            }
            if (time>=400) {
                moveUp = false;
            }
            if (time<=-400) {
                moveUp = true;
            }


            if (legTime == 19) {
                addTime = false;
            }
            else if (legTime == 0) {
                addTime = true;
            }
            if (addTime) {
                legTime = (legTime+1)%20;
            }
            else {
                legTime = (legTime-1)%20;
            }

            this.animate(time);
            this.angleOfDegree = (this.angleOfDegree + 1)%360;

            renderer.draw(root,modelView);
        }
    }




    @Override
    public void addPolygonMesh(String name, util.PolygonMesh<VertexType> mesh)
    {
        meshes.put(name,mesh);
    }




    @Override
    public void animate(float time) {

        INode groupL = nodes.get("LSpider");
        INode groupR = nodes.get("RSpider");
        INode aircraft = nodes.get("aircraft");
        INode airScrew = nodes.get("Craft-airscrew-position");

        aircraft.setAnimationTransform(
          new Matrix4f()
            .rotate((float)Math.toRadians(angleOfDegree),0,1,0)
            .translate(200,50,0)
            .rotate((float)Math.toRadians(-30), 1, 0, 0)
            .rotate((float)Math.toRadians(-90), 1, 0, 0));

        airScrew.setAnimationTransform(new Matrix4f()
          .rotate(angleOfDegree, 0, 1, 0));

        INode LL1 = nodes.get("L-left-leg1");
        INode LL2 = nodes.get("L-left-leg2");
        INode LL3 = nodes.get("L-left-leg3");
        INode LL4 = nodes.get("L-left-leg4");

        INode LR1 = nodes.get("L-right-leg1");
        INode LR2 = nodes.get("L-right-leg2");
        INode LR3 = nodes.get("L-right-leg3");
        INode LR4 = nodes.get("L-right-leg4");

        INode RL1 = nodes.get("R-left-leg1");
        INode RL2 = nodes.get("R-left-leg2");
        INode RL3 = nodes.get("R-left-leg3");
        INode RL4 = nodes.get("R-left-leg4");

        INode RR1 = nodes.get("R-right-leg1");
        INode RR2 = nodes.get("R-right-leg2");
        INode RR3 = nodes.get("R-right-leg3");
        INode RR4 = nodes.get("R-right-leg4");

        groupL
          .setAnimationTransform(
            new Matrix4f()
                .translate(time, 0, 0)
                .rotate((float)Math.toRadians(-90), 0, 1, 0)
                .rotate((float)Math.toRadians(-90), 1, 0, 0));
        groupR
          .setAnimationTransform(
            new Matrix4f()
              .translate(0, 0, time)
              .rotate((float)Math.toRadians(-90), 1, 0, 0)
              .rotate((float)Math.toRadians(180), 0, 0, 1));

        RL4
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(legTime), 1, 0, 0));
        RR4
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(-10), 1, 0, 0)
              .rotate((float)Math.toRadians(legTime), 1, 0, 0));

        RL3
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(20), 1, 0, 0)
              .rotate(-1*(float)Math.toRadians(legTime), 1, 0, 0));
        RR3
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(-10), 1, 0, 0)
              .rotate(-1*(float)Math.toRadians(legTime), 1, 0, 0));

        RL2
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(10), 1, 0, 0)
              .rotate((float)Math.toRadians(legTime), 1, 0, 0));
        RR2
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(legTime), 1, 0, 0));

        RL1
          .setAnimationTransform(
            new Matrix4f()
              .rotate(-1*(float)Math.toRadians(-20), 1, 0, 0)
              .rotate(-1*(float)Math.toRadians(legTime), 1, 0, 0));
        RR1
          .setAnimationTransform(
            new Matrix4f()
              .rotate(-1*(float)Math.toRadians(legTime), 1, 0, 0));



        LL4
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(legTime), 1, 0, 0));
        LR4
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(-10), 1, 0, 0)
              .rotate((float)Math.toRadians(legTime), 1, 0, 0));

        LL3
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(20), 1, 0, 0)
              .rotate(-1*(float)Math.toRadians(legTime), 1, 0, 0));
        LR3
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(-10), 1, 0, 0)
              .rotate(-1*(float)Math.toRadians(legTime), 1, 0, 0));

        LL2
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(10), 1, 0, 0)
              .rotate((float)Math.toRadians(legTime), 1, 0, 0));
        LR2
          .setAnimationTransform(
            new Matrix4f()
              .rotate((float)Math.toRadians(legTime), 1, 0, 0));

        LL1
          .setAnimationTransform(
            new Matrix4f()
              .rotate(-1*(float)Math.toRadians(-20), 1, 0, 0)
              .rotate(-1*(float)Math.toRadians(legTime), 1, 0, 0));
        LR1
          .setAnimationTransform(
            new Matrix4f()
              .rotate(-1*(float)Math.toRadians(legTime), 1, 0, 0));


    }

    @Override
    public void addNode(String name, INode node) {
        nodes.put(name,node);
    }


    @Override
    public INode getRoot() {
        return root;
    }

    @Override
    public Map<String, PolygonMesh<VertexType>> getPolygonMeshes() {
       Map<String,util.PolygonMesh<VertexType>> meshes = new HashMap<String,PolygonMesh<VertexType>>(this.meshes);
        return meshes;
    }

    @Override
    public Map<String, INode> getNodes() {
        Map<String,INode> nodes = new TreeMap<String,INode>();
        nodes.putAll(this.nodes);
        return nodes;
    }

    @Override
    public void addTexture(String name, String path) {
        this.textures.put(name,path);
    }

    @Override
    public int getTextureSize() {
        return this.textures.size();
    }



}
