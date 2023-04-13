import java.util.Random;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/*
*This algorithm is based of this paper
*An algorithm similar to this was used to simulate the Belousov Zhabotinsky reaction
*A very intresting chemical reaction with oscillations.
*Dewdney, A. K. “COMPUTER RECREATIONS.” 
*Scientific American 259, no. 2 (1988): 104–7. http://www.jstor.org/stable/24989205.
*/


public class Application extends JFrame
{
    public static final int SCREENHEIGHT = 1080;
    public static final int SCREENWIDTH = 1080;
    private static final double PI = 3.1415926535;

    public static Random rand;
    private Image dbImage;
    private Graphics dbg;

    private SwingWorker gameLooper;
    private boolean stopGameLoop;
    private boolean simulate;

    private long frameTime;
    
    private int frameCount;
    private int brushSize;

    private double[][] grid = new double[SCREENHEIGHT/2][SCREENWIDTH/2];

    public static boolean[] keys = {false, false, false};
    
    public boolean debugMode;

    public int k1, k2, g;
    
    public Application(boolean dbm) 
    {
        debugMode = dbm;

        addKeyListener(new KeyAdapter() {
          public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            switch (e.getKeyCode())
            {
                case KeyEvent.VK_UP:
                    brushSize = brushSize*2;
                    System.out.println("Brushsize: " + brushSize);
                    break;
                case KeyEvent.VK_DOWN:
                    brushSize = brushSize/2;
                    System.out.println("Brushsize: " + brushSize);
                    break;
                case KeyEvent.VK_I:
                    k1 += 1;
                    System.out.println("K1: " + k1);
                    break;
                case KeyEvent.VK_K:
                    k1 -= k1-1 > 0 ? 1 : 0;
                    System.out.println("K1: " + k1);
                    break;
                case KeyEvent.VK_U:
                    k2 += 1;
                    System.out.println("K2: " + k2);
                    break;
                case KeyEvent.VK_J:
                    k2 -= k2-1 > 0 ? 1 : 0;
                    System.out.println("K2: " + k2);
                    break;
                case KeyEvent.VK_Y:
                    g += 2;
                    System.out.println("G: " + g);
                    break;
                case KeyEvent.VK_H:
                    g -= 2;
                    System.out.println("G: " + g);
                    break;
                case KeyEvent.VK_R:
                    genBoard();
                    System.out.println("Board Regenerated");
                    break;
                case KeyEvent.VK_C:
                    clearBoard();
                    System.out.println("Board cleared");
                    break;

            }
            brushSize = clampInt(brushSize, 1, 256);
            
          }
        });

        addMouseListener(new MouseClickHandler()); 
        setSize(SCREENWIDTH, SCREENHEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setTitle("Simulator");
        setBackground(Color.BLACK);

        rand = new Random();
        frameCount = 0;
        stopGameLoop = false;
        brushSize = 2;

        k1 = 1; //Parameter k1
        k2 = 1; //Parameter k2
        g = 8; //Constant "infection rate"

        genBoard();

        gameLooper = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                             
                while(!stopGameLoop) {
                    update();
                    repaint();
                    Thread.sleep(17); //17ms = ~60 FPS
                }
                return null;
            }
        };
        
        gameLooper.execute();
    }


    private class MouseClickHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {

            int xPos = event.getX();
            int yPos = event.getY();

            //Possibly the most inefficent way of drawing a circle. TODO: Fix this
            for (int siz = brushSize; siz > 0; siz = siz - 1)
            {
                    for (double i = 0; i <= 360; i += 0.5)
                    {   
                        int x1 = (int)(siz * Math.cos(i * PI / 180));
                        int y1 = (int)(siz * Math.sin(i * PI / 180));
                        try
                        {
                        if (event.getButton() == 1)
                        {
                            grid[(xPos+x1)/2][(yPos+y1)/2] = 200;
                        }
                        else
                            grid[(xPos+x1)/2][(yPos+y1)/2] = 1;
                    }
                    catch (Exception e)
                    {
                        continue;
                    }
                }
            }
        }
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
        frameTime = (endFrame - startFrame)/1000000;
        
    }

    public void paintComponent(Graphics g)
    {
        for (int r = 0; r < grid.length; r++)
        {
            for (int c = 0; c < grid[r].length; c++)
            {
                int cn = (int)clampDouble(grid[r][c],0,255);
                g.setColor(new Color(cn > 100 ? cn : 0, cn > 200 ? cn : 0, (int)(cn/1.5)));
                g.fillRect(r*2,c*2,2,2);
            }

        }
        g.setColor(Color.BLACK);
        g.fillRect(0,25,100,100);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial Black", Font.BOLD, 20));
        g.drawString(String.valueOf(frameTime) + "MS", 10, 60);
        g.drawString("K1: " + k1, 10, 80);
        g.drawString("K2: " + k2, 10, 100);
        g.drawString("G:" + this.g, 10, 120);
    }
    
    public void update()
    {
        frameCount++;
        double sum = 0;
        int a = 0;
        int b = 0;

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
                    grid[r][c] = (sum / (a + b) + 1) + g;
                }
                
             }
        }
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

    public void genBoard()
    {
        for (int r = 0; r < grid.length; r++)
        {
            for (int c = 0; c < grid[r].length; c++)
            {
                grid[r][c] = rand.nextInt(0,256);
            }
        }
    }

    public void clearBoard()
    {
        for (int r = 0; r < grid.length; r++)
        {
            for (int c = 0; c < grid[r].length; c++)
            {
                grid[r][c] = 0;
            }
        }
    }

    public static double clampDouble(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
    public static int clampInt(int  val, int  min, int max) {
        return Math.max(min, Math.min(max, val));
    }

}