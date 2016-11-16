import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import sgraph.INode;
import util.Light;
import util.ObjectInstance;
import util.VertexProducer;


/**
 * Created by ashesh on 9/18/2015.
 *
 * The View class is the "controller" of all our OpenGL stuff. It cleanly encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View
{
    private enum TypeOfCamera {GLOBAL,OBJECT, KEYBOARD};
    private int WINDOW_WIDTH,WINDOW_HEIGHT;
    private Stack<Matrix4f> modelView;
    private Matrix4f projection,trackballTransform;
    private float trackballRadius;
    private Vector2f mousePos;

    private Map<String, ObjectInstance> meshObjects;
    private Map<String,Matrix4f> meshTransforms;
    private Map<String,util.Material> meshMaterials;
    private List<Light> lights;
    //0-meshObjects.size()-1 are object coordinates, then world and then view
    private List<Integer> lightCoordinateSystems;

    private util.ShaderProgram program;
    private util.ShaderLocationsVault shaderLocations;
    private int projectionLocation;
    private sgraph.IScenegraph<VertexAttrib> scenegraph;

    private TypeOfCamera cameraMode;


    public View()
    {
        projection = new Matrix4f();
        modelView = new Stack<Matrix4f>();
        trackballRadius = 300;
        trackballTransform = new Matrix4f();
        scenegraph = null;
        lights = new ArrayList<util.Light>();
        lightCoordinateSystems = new ArrayList<Integer>();
        meshObjects = new HashMap<String, ObjectInstance>();
        meshTransforms = new HashMap<String,Matrix4f>();
        meshMaterials = new HashMap<String,util.Material>();

        cameraMode = TypeOfCamera.GLOBAL;
    }

    private void initObjects(GL3 gl) throws FileNotFoundException
    {
        util.PolygonMesh mesh;
        util.ObjectInstance o;
        Matrix4f transform;
        util.Material mat;
        VertexProducer<?> vertexProducer = new VertexAttribProducer();

        InputStream in;

        Map<String, String> shaderToVertexAttribute = new HashMap<String, String>();

        //currently there is only one per-vertex attribute: position
        shaderToVertexAttribute.put("vPosition", "position");


        mat =  new util.Material();

        //wall by 1000*1000
        in = getClass().getClassLoader()
          .getResourceAsStream
            ("models/box.obj");
//        in = new FileInputStream("models/box.obj");
        mesh = util.ObjImporter.importFile(vertexProducer,in,false);
        o = new util.ObjectInstance(gl,
          program,
          shaderLocations,
          shaderToVertexAttribute,
          mesh,new String("Wall"));
        mat.setAmbient(1,.5f,0); //only this one is used currently to determine color
        mat.setDiffuse(1,.5f,0);
        mat.setSpecular(1,.5f,0);

        meshObjects.put("Wall",o);
        meshMaterials.put("Wall",new util.Material(mat));
        transform = new Matrix4f().translate(0,0,-40)
          .scale(1000,1000,1);
        meshTransforms.put("Wall",transform);


        //floor by 10000*10000
        in = getClass().getClassLoader()
          .getResourceAsStream
            ("models/box.obj");
//        in = new FileInputStream("models/box.obj");
        mesh = util.ObjImporter.importFile(vertexProducer,in,false);
        o = new util.ObjectInstance(gl,
          program,
          shaderLocations,
          shaderToVertexAttribute,
          mesh,new String("Floor"));
        mat.setAmbient(1,.5f,0); //only this one is used currently to determine color
        mat.setDiffuse(1,.5f,0);
        mat.setSpecular(1,.5f,0);

        meshObjects.put("Floor",o);
        meshMaterials.put("Floor",new util.Material(mat));
        transform = new Matrix4f().translate(0,-500,0)
          .scale(10000,1,10000);
        meshTransforms.put("Floor",transform);
    }

    public void initScenegraph(GLAutoDrawable gla,InputStream in) throws Exception
    {
        GL3 gl = gla.getGL().getGL3();

        if (scenegraph!=null)
            scenegraph.dispose();

        program.enable(gl);

        scenegraph = sgraph.SceneXMLReader.importScenegraph(in,new VertexAttribProducer());
        sgraph.IScenegraphRenderer renderer = new sgraph.GL3ScenegraphRenderer();
        renderer.setContext(gla);
        Map<String,String> shaderVarsToVertexAttribs = new HashMap<String,String>();
        shaderVarsToVertexAttribs.put("vPosition","position");
        shaderVarsToVertexAttribs.put("vNormal","normal");
        shaderVarsToVertexAttribs.put("vTexCoord","texcoord");
        renderer.initShaderProgram(program,shaderVarsToVertexAttribs);
        scenegraph.setRenderer(renderer);       //order matters

        program.disable(gl);
    }

    private void initLights() {
        util.Light l = new util.Light();
        l.setAmbient(0.9f, 0.9f, 0.9f);
        l.setDiffuse(0.9f, 0.9f, 0.9f);
        l.setSpecular(0.9f, 0.9f, 0.9f);
        l.setPosition(00, 00, 100);
        lights.add(l);
        //read comments above where this list is declared
        lightCoordinateSystems.add(meshObjects.size()); //world

    }

    public void init(GLAutoDrawable gla) throws Exception
    {
        GL3 gl = gla.getGL().getGL3();




        //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
        program = new util.ShaderProgram();

        program.createProgram(gl,"shaders/phong-multiple.vert","shaders/phong-multiple.frag");

        shaderLocations = program.getAllShaderVariables(gl);

        //get input variables that need to be given to the shader program
        projectionLocation = shaderLocations.getLocation("projection");

        initObjects(gl);
        initLights();
    }



    public void draw(GLAutoDrawable gla)
    {
        GL3 gl = gla.getGL().getGL3();
        FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);
        FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);

        gl.glClearColor(0,0,0, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

        program.enable(gl);

        while (!modelView.empty())
            modelView.pop();

        /*
         *In order to change the shape of this triangle, we can either move the vertex positions above, or "transform" them
         * We use a modelview matrix to store the transformations to be applied to our triangle.
         * Right now this matrix is identity, which means "no transformations"
         */
        modelView.push(new Matrix4f());


        if (cameraMode == TypeOfCamera.GLOBAL)

            // where the camera actually is
            // reference: direction of what's up
            modelView
              .peek()               //50//80
              .lookAt(new Vector3f(0,0,400),new Vector3f(0,0,0),new Vector3f(0,1,0))
              .mul(trackballTransform);
        else if (cameraMode == TypeOfCamera.OBJECT){

            // camera matrix
            Matrix4f camera = new Matrix4f(modelView.peek());
            INode plane = scenegraph.getNodes().get("Craft-forCamera");
            if (plane == null) {
                throw new NullPointerException("Cannot find the head");
            }
            Matrix4f planeToWorld = plane.objectToView(new Matrix4f(), new Matrix4f(camera));
            Matrix4f worldToPlane = new Matrix4f(planeToWorld).invert();

            modelView.peek()
              .lookAt(new Vector3f(0,0,-5),new Vector3f(0,0,-20),new Vector3f(0,1,0))
              .mul(worldToPlane);
        }



    /*
     *Supply the shader with all the matrices it expects.
    */
        FloatBuffer fb = Buffers.newDirectFloatBuffer(16);
        gl.glUniformMatrix4fv(projectionLocation,1,false,projection.get(fb));
        //return;
        scenegraph.draw(modelView);
    /*
     *OpenGL batch-processes all its OpenGL commands.
          *  *The next command asks OpenGL to "empty" its batch of issued commands, i.e. draw
     *
     *This a non-blocking function. That is, it will signal OpenGL to draw, but won't wait for it to
     *finish drawing.
     *
     *If you would like OpenGL to start drawing and wait until it is done, call glFinish() instead.
     */
        gl.glFlush();

        program.disable(gl);



    }

    public void mousePressed(int x,int y)
    {
        mousePos = new Vector2f(x,y);
    }

    public void mouseReleased(int x,int y)
    {
        System.out.println("Released");
    }

    public void mouseDragged(int x,int y) {
        Vector2f newM = new Vector2f(x,y);

        Vector2f delta = new Vector2f(newM.x-mousePos.x,newM.y-mousePos.y);
        mousePos = new Vector2f(newM);

        trackballTransform
          = new Matrix4f()
          .rotate(delta.x/trackballRadius,0,1,0)
          .rotate(delta.y/trackballRadius,1,0,0)
          .mul(trackballTransform);
    }

    public void reshape(GLAutoDrawable gla,int x,int y,int width,int height)
    {
        GL gl = gla.getGL();
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
        gl.glViewport(0, 0, width, height);

        projection = new Matrix4f().perspective((float)Math.toRadians(120.0f),(float)width/height,0.1f,10000.0f);
       // proj = new Matrix4f().ortho(-400,400,-400,400,0.1f,10000.0f);

    }

    public void dispose(GLAutoDrawable gla)
    {
        GL3 gl = gla.getGL().getGL3();

    }

    public void setGlobal() {
        this.cameraMode = TypeOfCamera.GLOBAL;
    }

    public void setObject() {
        this.cameraMode = TypeOfCamera.OBJECT;
    }



}
