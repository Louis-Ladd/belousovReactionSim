import java.util.Random;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.*;


public class Application extends JFrame
{
    public static final int SCREENHEIGHT = 1200;
    public static final int SCREENWIDTH = 1200;

    public static Random rand;
    private Image dbImage;
    private Graphics dbg;

    private SwingWorker gameLooper;
    private boolean stop;
    
    private int frameCount;

    private double[][] grid = new double[SCREENHEIGHT][SCREENWIDTH];

    public static boolean[] keys = {false, false, false};
    
    public boolean debugMode;

    public int k1, k2;
    
    public Application(boolean dbm) 
    {
        debugMode = dbm;
        addKeyListener(new KeyAdapter() {
          public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            switch (e.getKeyCode())
            {
                case KeyEvent.VK_UP:
                    keys[2] = true;
                    break;
                case KeyEvent.VK_LEFT:
                    keys[0] = true;
                    break;
                case KeyEvent.VK_RIGHT:
                    keys[1] = true;
                    break;
            }
          }
            public void keyReleased(KeyEvent e) {
            int keyCode = e.getKeyCode();
            switch (e.getKeyCode())
            {
                case KeyEvent.VK_UP:
                    keys[2] = false;
                    break;
                case KeyEvent.VK_LEFT:
                    keys[0] = false;
                    break;
                case KeyEvent.VK_RIGHT:
                    keys[1] = false;
                    break;
            }
          }
        });


        setSize(SCREENWIDTH, SCREENHEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setTitle("Simulator");
        setBackground(Color.BLACK);
        rand = new Random();
        frameCount = 0;
        stop = false;

        k1 = 1;
        k2 = 6;

        for (int r = 0; r < grid.length; r++)
        {
            for (int c = 0; c < grid[r].length; c++)
            {
                grid[r][c] = rand.nextInt(0,256);
            }
        }

        gameLooper = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                while(!stop) {
                    update();
                    repaint();
                    Thread.sleep(51); //17ms = ~60 FPS
                }
                return null;
            }
        };
        
        gameLooper.execute();
    }

    @Override
    public void paint(Graphics g) {
        //Double Buffered image
        long startFrame = System.nanoTime();

        dbImage = createImage(getWidth(), getHeight());
        dbg = dbImage.getGraphics();

        paintComponent(dbg);
        
        g.drawImage(dbImage, 0, 0, this);

        long endFrame = System.nanoTime();
        System.out.println(""+((endFrame - startFrame)/1000000) +" MS");
        
    }

    public void paintComponent(Graphics g)
    {
        for (int r = 0; r < grid.length; r++) //Draw from top of list to keep "z distance" constant.
        {
            for (int c = 0; c < grid[r].length; c++)
            {
                int cn = (int)clamp(grid[r][c],0,255);
                g.setColor(new Color(cn, cn, cn));
                g.fillRect(r*8,c*8,8,8);
            }

        }
    }
    
    public void update()
    {
        frameCount++;
        double sum = 0;
        int a = 0;
        int b = 0;

        if (false)
        {
            return;
        }

        for (int r = 0; r < grid.length; r++)
        {
            for (int c = 0; c < grid[r].length; c++)
            {
                sum = sumNeighbors(r,c);
                a = getNumInfected(r,c);
                b = getNumSick(r,c);

                if (isCellHealthy(r,c))
                {
                    grid[r][c] = (a/k1) + (b/k2) + 1;
                }

                else if (isCellSick(r,c))
                {
                    grid[r][c] = 1;
                }

                else if (isCellInfected(r,c))
                {
                    grid[r][c] = (sum / (a + b) + 1) + 15;
                }
                
             }
        }
    }

    public void keyTyped(KeyEvent e)
    {
        System.out.print(e);
    }

    public boolean isCellHealthy(int r, int c) {
        return (grid[r][c] <= 1);
    }
    

    public boolean isCellInfected(int r, int c) {
        return (grid[r][c] > 1 && grid[r][c] < 255);
    }
    
    public boolean isCellSick(int r, int c) {
        return (grid[r][c] >= 254);
    }

    public double sumNeighbors(int r, int c)
    {
        double sum = 0; 

        for (int i = -1; i <= 1; i++)
        {
            for (int j = -1; j <= 1; j++)
            {
                if (!isInBounds(r+i, c+j))
                {
                    continue;
                }

                if ((i == 0 && j == 0)) //Moore neighborhood
                {
                    continue;
                }

                sum += grid[r+i][c+j];
            }
        }
        return sum;
    }

    public int getNumSick(int r, int c)
    {
        int a = 0;
        for (int i = -1; i <= 1; i++)
        {
            for (int j = -1; j <= 1; j++)
            {
                if (!isInBounds(r+i, c+j))
                {
                    continue;
                }

                if ((i == 0 && j == 0))
                {
                    continue;
                }

                if (isCellInfected(r+i, c+j))
                {
                    a += 1;
                }

            }
        }
        return a;
    }

    public int getNumInfected(int r, int c)
    {
        int b = 0;

        for (int i = -1; i <= 1; i++)
        {
            for (int j = -1; j <= 1; j++)
            {
                if (!isInBounds(r+i, c+j))
                {
                    continue;
                }

                if ((i == 0 && j == 0))
                {
                    continue;
                }

                if (isCellSick(r+i, c+j))
                {
                    b += 1;
                }

            }
        }

        return b;
    }

    public boolean isInBounds(int i, int j)
    {
        if (i > 0 &&
            i < grid.length - 1 &&
            j > 0 &&
            j < grid[0].length - 1)
        {
            return true;
        }
        return false;
    }

    public static double clamp (double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

}