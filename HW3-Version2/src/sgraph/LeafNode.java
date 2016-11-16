package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import util.Light;

/**
 * This node represents the leaf of a scene graph. It is the only type of node that has
 * actual geometry to render.
 * @author Amit Shesh
 */
public class LeafNode extends AbstractNode
{
    /**
     * The name of the object instance that this leaf contains. All object instances are stored
     * in the scene graph itself, so that an instance can be reused in several leaves
     */
    protected String objInstanceName;
    /**
     * The material associated with the object instance at this leaf
     */
    protected util.Material material;


    protected String textureName;

    public LeafNode(String instanceOf,IScenegraph graph,String name)
    {
        super(graph,name);
        this.objInstanceName = instanceOf;
    }



    /*
	 *Set the material of each vertex in this object
	 */
    @Override
    public void setMaterial(util.Material mat)
    {
        material = new util.Material(mat);
    }

  /**
   * add a light to this leaf node
   * @param light
   */
  @Override
    public void addLight(Light light) {

      lights.add(light);
    }


    /**
     * Set texture ID of the texture to be used for this leaf
     * @param name
     */
    @Override
    public void setTextureName(String name)
    {
        textureName = name;
    }

    /*
     * gets the material
     */
    public util.Material getMaterial()
    {
        return material;
    }

    @Override
    public INode clone()
    {
        LeafNode newclone = new LeafNode(this.objInstanceName,scenegraph,name);
        newclone.setMaterial(this.getMaterial());
        return newclone;
    }


    /**
     * Delegates to the scene graph for rendering. This has two advantages:
     * <ul>
     *     <li>It keeps the leaf light.</li>
     *     <li>It abstracts the actual drawing to the specific implementation of the scene graph renderer</li>
     * </ul>
     * @param context the generic renderer context {@link sgraph.IScenegraphRenderer}
     * @param modelView the stack of modelview matrices
     * @throws IllegalArgumentException
     */
    @Override
    public void draw(IScenegraphRenderer context,Stack<Matrix4f> modelView) throws IllegalArgumentException
    {
        if (objInstanceName.length()>0)
        {

            context.drawMesh(objInstanceName,material,textureName,modelView.peek());
        }
    }

  @Override
  public void descendLights(List<Light> result, Matrix4f modelView) {

    if (objInstanceName.length()>0)
    {

      Matrix4f objectToView = objectToView(new Matrix4f(), new Matrix4f(modelView));
      List<Light> transformedLights = new ArrayList<>();
      for (Light light: lights) {
        transformedLights.add(light);
      }
      transformedToLight(objectToView, transformedLights);

      result.addAll(transformedLights);
    }


  }

  @Override
  public Matrix4f objectToView(Matrix4f acc, Matrix4f modelView) {
    if (this.parent == null) {
      return new Matrix4f().mul(modelView).mul(acc);
    }
    else {
      return this.parent.objectToView(acc, modelView);
    }

  }




}
