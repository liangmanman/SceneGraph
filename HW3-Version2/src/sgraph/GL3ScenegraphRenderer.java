package sgraph;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.texture.Texture;

import org.joml.Matrix4f;
import util.IVertexData;
import util.Light;
import util.TextureImage;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * This is a scene graph renderer implementation that works specifically with the JOGL library
 * It mandates OpenGL 3 and above.
 * @author Amit Shesh
 */
public class GL3ScenegraphRenderer implements IScenegraphRenderer {
    /**
     * The JOGL specific rendering context
     */
    private GLAutoDrawable glContext;
    /**
     * A table of shader locations and variable names
     */
    protected util.ShaderLocationsVault shaderLocations;
    /**
     * A table of shader variables -> vertex attribute names in each mesh
     */
    protected Map<String,String> shaderVarsToVertexAttribs;

    /**
     * A map to store all the textures
     */
    private Map<String, TextureImage> textures;
    /**
     * A table of renderers for individual meshes
     */
    private Map<String,util.ObjectInstance> meshRenderers;

    /**
     * A variable tracking whether shader locations have been set. This must be done before
     * drawing!
     */
    private boolean shaderLocationsSet;


    class LightLocation {
        int ambient, diffuse, specular, position, spotDirection, spotAngle;

        public LightLocation() {
            ambient = diffuse = specular = position = spotDirection = spotAngle= -1;
        }
    }
    private Matrix4f proj;
    private int modelviewLocation, projectionLocation, normalmatrixLocation, texturematrixLocation;
    private int materialAmbientLocation, materialDiffuseLocation, materialSpecularLocation, materialShininessLocation;
    private int textureLocation;
    private List<LightLocation> lightLocations;
    private int numLightsLocation;
    private List<Light> lights;






    public GL3ScenegraphRenderer()
    {
        meshRenderers = new HashMap<String,util.ObjectInstance>();
        shaderLocations = new util.ShaderLocationsVault();
        shaderLocationsSet = false;

        lights = new ArrayList<Light>();
        lightLocations = new ArrayList<LightLocation>();
        proj = new Matrix4f();
        proj.identity();
        textures = new HashMap<String,TextureImage>();
    }

    /**
     * Specifically checks if the passed rendering context is the correct JOGL-specific
     * rendering context {@link com.jogamp.opengl.GLAutoDrawable}
     * @param obj the rendering context (should be {@link com.jogamp.opengl.GLAutoDrawable})
     * @throws IllegalArgumentException if given rendering context is not {@link com.jogamp.opengl.GLAutoDrawable}
     */
    @Override
    public void setContext(Object obj) throws IllegalArgumentException
    {
        if (obj instanceof GLAutoDrawable)
        {
            glContext = (GLAutoDrawable)obj;
        }
        else
            throw new IllegalArgumentException("Context not of type GLAutoDrawable");
    }

    /**
     * Add a mesh to be drawn later.
     * The rendering context should be set before calling this function, as this function needs it
     * This function creates a new
     * {@link util.ObjectInstance} object for this mesh
     * @param name the name by which this mesh is referred to by the scene graph
     * @param mesh the {@link util.PolygonMesh} object that represents this mesh
     * @throws Exception
     */
    @Override
    public <K extends IVertexData> void addMesh(String name, util.PolygonMesh<K> mesh) throws Exception
    {
        if (!shaderLocationsSet)
            throw new Exception("Attempting to add mesh before setting shader variables. Call initShaderProgram first");
        if (glContext==null)
            throw new Exception("Attempting to add mesh before setting GL context. Call setContext and pass it a GLAutoDrawable first.");

        //verify that the mesh has all the vertex attributes as specified in the map
        if (mesh.getVertexCount()<=0)
            return;
        K vertexData = mesh.getVertexAttributes().get(0);
      GL3 gl = glContext.getGL().getGL3();

      for (Map.Entry<String,String> e:shaderVarsToVertexAttribs.entrySet()) {
            if (!vertexData.hasData(e.getValue()))
                throw new IllegalArgumentException("Mesh does not have vertex attribute "+e.getValue());
        }
      util.ObjectInstance obj = new util.ObjectInstance(gl,
              shaderLocations,shaderVarsToVertexAttribs,mesh,name);

      meshRenderers.put(name,obj);
    }

    @Override
    public void addTexture(String name,String path)
    {
        TextureImage image = null;
        String imageFormat = path.substring(path.indexOf('.')+1);
        try {
            image = new TextureImage(path,imageFormat,name);
        } catch (IOException e) {
            throw new IllegalArgumentException("Texture "+path+" cannot be read!");
        }
        textures.put(name,image);
    }

    /**
     * Begin rendering of the scene graph from the root
     * @param root
     * @param modelView
     */
    @Override
    public void draw(INode root, Stack<Matrix4f> modelView)
    {

        root.draw(this,modelView);
        List<Light> allLightsDescend = new ArrayList<>();
        root.descendLights(allLightsDescend, modelView.peek());
        setLights(allLightsDescend);

    }



    @Override
    public void dispose()
    {
        for (util.ObjectInstance s:meshRenderers.values())
            s.cleanup(glContext);
    }
    /**
     * Draws a specific mesh.
     * If the mesh has been added to this renderer, it delegates to its correspond mesh renderer
     * This function first passes the material to the shader. Currently it uses the shader variable
     * "vColor" and passes it the ambient part of the material. When lighting is enabled, this method must
     * be overriden to set the ambient, diffuse, specular, shininess etc. values to the shader
     * @param name
     * @param material
     * @param transformation
     */
    @Override
    public void drawMesh(String name, util.Material material,String textureName,final Matrix4f transformation) {
        if (meshRenderers.containsKey(name))
        {
            GL3 gl = glContext.getGL().getGL3();
            //get the color

            FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
            FloatBuffer fb = Buffers.newDirectFloatBuffer(4);

//            int loc = shaderLocations.getLocation("vColor");
////            int loc = shaderLocations.getLocation("material.ambient");
//            //set the color for all vertices to be drawn for this object
//            if (loc<0)
//                throw new IllegalArgumentException("No shader variable for \" vColor \"");
//
//            gl.glUniform3fv(loc,1,material.getAmbient().get(fb));
//
//            loc = shaderLocations.getLocation("modelview");
//            if (loc<0)
//                throw new IllegalArgumentException("No shader variable for \" modelview \"");
//
//            fb = Buffers.newDirectFloatBuffer(16);
//            transformation.get(fb);
//            gl.glUniformMatrix4fv(loc,1,false,transformation.get(fb));


            Matrix4f normalmatrix = new Matrix4f(transformation);
            normalmatrix = normalmatrix.invert().transpose();
            if (materialDiffuseLocation< 0) {
                throw new IllegalArgumentException("No shader variable for \" modelviewLocation \"");
            }

            gl.glUniformMatrix4fv(modelviewLocation, 1, false, transformation.get(fb16));
            gl.glUniformMatrix4fv(normalmatrixLocation, 1, false, normalmatrix.get(fb16));

            gl.glUniform3fv(materialAmbientLocation, 1, material.getAmbient().get(fb));
            gl.glUniform3fv(materialDiffuseLocation, 1, material.getDiffuse().get(fb));
            gl.glUniform3fv(materialSpecularLocation, 1, material.getSpecular().get(fb));
            gl.glUniform1f(materialShininessLocation, material.getShininess());

            Matrix4f textureTransform = new Matrix4f();
            if(textures.containsKey(textureName)) {
                TextureImage textureImage = textures.get(textureName);

                Texture texture = textureImage.getTexture();

                if (texture.getMustFlipVertically()) {//for flipping the image vertically
                    textureTransform = new Matrix4f().translate(0,1,0).scale(1,-1,1);
                }
                texture.bind(gl);
            }
            else {
                // no texture: give white texture
                if(! textures.containsKey("white")) {
                    throw new IllegalArgumentException("have to pass a white texture first");
                }
                TextureImage textureImage = textures.get("white");
                Texture texture = textureImage.getTexture();
                if (!texture.getMustFlipVertically()) {
                    //for flipping the image vertically
                    textureTransform = new Matrix4f().translate(0,1,0).scale(1,-1,1);
                }
                texture.bind(gl);
            }

            gl.glUniformMatrix4fv(texturematrixLocation, 1, false, textureTransform.get(fb16));

            meshRenderers.get(name).draw(glContext);
        }
    }



    /**
     * Queries the shader program for all variables and locations, and adds them to itself
     * @param shaderProgram
     */
    @Override
    public void initShaderProgram(util.ShaderProgram shaderProgram,Map<String,String> shaderVarsToVertexAttribs)
    {

        Objects.requireNonNull(glContext);
        GL3 gl = glContext.getGL().getGL3();



        shaderLocations = shaderProgram.getAllShaderVariables(gl);
        this.shaderVarsToVertexAttribs = new HashMap<String,String>(shaderVarsToVertexAttribs);




        shaderLocationsSet = true;
        initShaderVariables();

    }


    @Override
    public void setLights(List<Light> lights) {
        GL3 gl = glContext.getGL().getGL3();
        FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);

        //all the light properties, except positions
        gl.glUniform1i(numLightsLocation, lights.size());
        for (int i = 0; i < lightLocations.size(); i++) {
            gl.glUniform4fv(lightLocations.get(i).position, 1, lights.get(i).getPosition().get(fb4));
            gl.glUniform3fv(lightLocations.get(i).ambient, 1, lights.get(i).getAmbient().get(fb4));
            gl.glUniform3fv(lightLocations.get(i).diffuse, 1, lights.get(i).getDiffuse().get(fb4));
            gl.glUniform3fv(lightLocations.get(i).specular, 1, lights.get(i).getSpecular().get(fb4));
            gl.glUniform4fv(lightLocations.get(i).spotDirection,1,lights.get(i).getSpotDirection().get(fb4));
            gl.glUniform1f(lightLocations.get(i).spotAngle, lights.get(i).getSpotCutoff());

        }




    }

    private void initShaderVariables() {
        //get input variables that need to be given to the shader program
        projectionLocation = shaderLocations.getLocation("projection");
        modelviewLocation = shaderLocations.getLocation("modelview");
        normalmatrixLocation = shaderLocations.getLocation("normalmatrix");
        texturematrixLocation = shaderLocations.getLocation("texturematrix");
        materialAmbientLocation = shaderLocations.getLocation("material.ambient");
        materialDiffuseLocation = shaderLocations.getLocation("material.diffuse");
        materialSpecularLocation = shaderLocations.getLocation("material.specular");
        materialShininessLocation = shaderLocations.getLocation("material.shininess");

        textureLocation = shaderLocations.getLocation("image");

        numLightsLocation = shaderLocations.getLocation("numLights");

        System.out.println("initShaderVariables() success?");
    }


    @Override
    public int getShaderLocation(String name)
    {
        return shaderLocations.getLocation(name);
    }

    @Override
    public void getAllLight(List<Light> allLights) {
        this.lights.addAll(allLights);
        for (int i = 0; i < lights.size(); i++) {
            LightLocation ll = new LightLocation();
            String name;

            name = "light[" + i + "]";
            ll.ambient = shaderLocations.getLocation(name + "" + ".ambient");
            ll.diffuse = shaderLocations.getLocation(name + ".diffuse");
            ll.specular = shaderLocations.getLocation(name + ".specular");
            ll.position = shaderLocations.getLocation(name + ".position");
            ll.spotDirection = shaderLocations.getLocation(name + ".spotDirection");
            ll.spotAngle = shaderLocations.getLocation(name + ".spotAngle");
            lightLocations.add(ll);
        }
        System.out.println("renderer get all lights and the size is " + lightLocations.size());
    }

    @Override
    public void addTexture(String name, TextureImage texture) throws Exception {
        if (!shaderLocationsSet)
            throw new Exception("Attempting to add texture before setting shader variables; wrong" +
              "order");
        if (glContext==null)
            throw new Exception("Attempting to add texture before setting GL context; should set " +
              "context first");
        textures.put(name,texture);

        TextureImage textureImage = textures.get(name);

        GL3 gl = glContext.getGL().getGL3();

        Texture newTexture = textureImage.getTexture();
        if (texture != null) {

            newTexture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            newTexture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
            newTexture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            newTexture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

        }
    }
}