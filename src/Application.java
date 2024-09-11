import java.lang.Math;
import java.util.Random;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/*
*This cellular automata is based of this paper
*this was used to simulate the Belousov Zhabotinsky reaction
*A very intresting chemical reaction with oscillations.
*Dewdney, A. K. "COMPUTER RECREATIONS."
*Scientific American 259, no. 2 (1988): 104–7. http://www.jstor.org/stable/24989205.
*/

public class Application extends JFrame
{
    public static final int SCREENHEIGHT = 1080;
    public static final int SCREENWIDTH = 1080;
    private static final double PI = 3.1415926535;
    private static final int SCALE = 3;
    private static final int messageLimit = 10;

    public static Random rand;

    private Image dbImage;
    private Graphics dbg;

    private SwingWorker gameLoop;

    private int frameCount, brushSize;

    private boolean stopGameLoop, simulate, highlightHealthy;

    private long frameTime;

    private int[][] grid = new int[SCREENHEIGHT/SCALE][SCREENWIDTH/SCALE];

    // These array lists are presumed to always be the same size and correlate to one another
    // A full class implementation felt overkill for this feature.
    private ArrayList<String> messages = new ArrayList<String>();
    private ArrayList<Integer> messageTimeout = new ArrayList<Integer>();

    public static boolean[] keys = {false, false, false};
    
    public boolean debugMode;

    public int k1Const, k2Const, gConst;
    
    public Application(boolean dbm) 
    {
        debugMode = dbm;

        addKeyListener(new KeyAdapter() {
          public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();
            switch (e.getKeyCode())
            {
                case KeyEvent.VK_UP:
                    brushSize = clampInt(brushSize*2, 1, 512);
                    newMessage("Brushsize: " + brushSize, 20);
                    break;
                case KeyEvent.VK_DOWN:
                    brushSize = clampInt(brushSize/2, 1, 512);
                    newMessage("Brushsize: " + brushSize, 20);
                    break;
                case KeyEvent.VK_I:
                    k1Const += 1;
                    break;
                case KeyEvent.VK_K:
                    k1Const -= k1Const-1 > 0 ? 1 : 0;
                    break;
                case KeyEvent.VK_U:
                    k2Const += 1;
                    break;
                case KeyEvent.VK_J:
                    k2Const -= k2Const-1 > 0 ? 1 : 0;
                    break;
                case KeyEvent.VK_Y:
                    gConst += gConst <= 255 ? 1 : 0;
                    break;
                case KeyEvent.VK_H:
                    gConst -= 1;
                    break;
                case KeyEvent.VK_R:
                    genBoard();
                    newMessage("Board Regenerated", 20);
                    break;
                case KeyEvent.VK_C:
                    clearBoard();
                    newMessage("Board cleared", 20);
                    break;
                case KeyEvent.VK_S:
                    if (!simulate)
                    {
                        update();
                        newMessage("step", 10);
                    }
                    break;
                case KeyEvent.VK_SPACE:
                    simulate = !simulate;
                    newMessage(simulate ? "unpaused" : "paused", 20);
                    break;
                case KeyEvent.VK_N:
                    highlightHealthy = !highlightHealthy;
                    newMessage(highlightHealthy ? "Highlight Healthy Enabled" : "Highlight Healthy Disabled" , 20);
                    break;

            }
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
        simulate = true;
        brushSize = 2;

        k1Const = 1; //Parameter k1
        k2Const = 6; //Parameter k2
        gConst = 6; //Constant "infection rate"

        gameLoop = new SwingWorker() 
        {
            @Override
            protected Object doInBackground() throws Exception 
            {
                newMessage("Proceed with caution if you are prone to epileptic seizures.", 11);
                newMessage("WARNING: High parameter values can cause intense flashing.", 11);

                Thread.sleep(2000);
                newMessage("Starting...", 11);
                repaint();
                Thread.sleep(500);

                genBoard();

                while(!stopGameLoop) 
                {
                    if (simulate)
                    {update();}
                    repaint();
                    Thread.sleep(51); //17ms = ~60 FPS
                }
                return null;
            }
        };

        gameLoop.execute();
    }

    private class MouseClickHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {

            int xPos = event.getX();
            int yPos = event.getY();

            //Possibly the second most inefficent way of drawing a circle. TODO: Fix this
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
                            grid[(xPos+x1)/SCALE][(yPos+y1)/SCALE] = 200;
                        }
                        else
                        {    
                            grid[(xPos+x1)/SCALE][(yPos+y1)/SCALE] = 1;
                        }
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
    public void paint(Graphics g) 
    {
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
        // vvv Handles drawing the grid vvv
        for (int r = 0; r < grid.length; r++)
        {
            for (int c = 0; c < grid[r].length; c++)
            {
                int cn = (int)clampDouble(grid[r][c],0,255);
                g.setColor(new Color(cn > 100 ? cn : 0, cn > 200 ? cn : 0, (int)(cn/1.5))); // Blue-Yellowish scale 
                //g.setColor(new Color(cn, cn, cn)); // Grayscale
                if (grid[r][c] <= 1 && highlightHealthy)
                {
                    g.setColor(Color.GREEN);
                }
                g.fillRect(r*SCALE,c*SCALE,SCALE,SCALE);
            }
        }

        // vvv Handles drawing information on top left vvv
        g.setColor(Color.BLACK);
        g.fillRect(0,25,90,100);
        g.setColor(Color.WHITE);

        // Must have monospaced fonts to have text backgrounds scale properly
        g.setFont(new Font("Monospaced", Font.BOLD, 20));

        int frameTimeInt = Math.toIntExact(frameTime);
        g.setColor(new Color(clampInt(25+(int)(frameTimeInt*2), 0, 255), clampInt(255-frameTimeInt, 0, 255), 0));
        g.drawString(String.valueOf(frameTime) + "MS", 10, 60);

        g.setColor(Color.WHITE);
        g.drawString("K1: " + k1Const, 10, 80);
        g.drawString("K2: " + k2Const, 10, 100);

        // Flashing gets really bad past 200
        if (gConst > 200)
        {g.setColor(Color.RED);}
        g.drawString("G:" + gConst, 10, 120);

        //vvv Handles pretty message popups vvv
        for (int i = 0; i < messages.size(); i++)
        {
            if (messageTimeout.get(i) <= 0)
            {
                messages.remove(i);
                messageTimeout.remove(i);
                i -= i > 0 ? 1 : 0;
                continue;
            }

            messageTimeout.set(i,messageTimeout.get(i)-1);

            int alpha = 255;

            //Fade out effect
            alpha = messageTimeout.get(i) <= 20 ? (int)(alpha * messageTimeout.get(i) / 10) : alpha;
            alpha = clampInt(alpha, 0,255);

            g.setColor(new Color(0, 0, 0, alpha));
            g.fillRect(((SCREENWIDTH/2) - (messages.get(i).length()*10)/2)-10, 
                        80 + (50*i), 
                        (12*messages.get(i).length()) + 20, 
                        30);

            g.setColor(new Color(255, 255, 255, alpha));
            g.drawString(messages.get(i), 
                        (SCREENWIDTH/2) - (messages.get(i).length()*10)/2, 
                        100 + (50*i));
        }
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
                    grid[r][c] = (int)(a/k1Const) + (int)(b/k2Const);
                }
                else if (isCellSick(r,c))
                {
                    grid[r][c] = 0;
                }
                else if (isCellInfected(r,c))
                {
                    grid[r][c] = (int)(sum / (a + b + 1)) + gConst;
                }
            }
        }

        if (messages.size() > messageLimit)
        {
            messages.remove(messages.size()-1);
            messageTimeout.remove(messageTimeout.size()-1);
        }
    }

    public boolean isCellHealthy(int r, int c) 
    {return (grid[r][c] <= 0);}
    
    public boolean isCellInfected(int r, int c) 
    {return (grid[r][c] > 0 && grid[r][c] < 255);}
    
    public boolean isCellSick(int r, int c) 
    {return (grid[r][c] >= 255);}

    public double sumNeighbors(int r, int c)
    {
        double sum = 0; 

        for (int i = -1; i <= 1; i++)
        {
            for (int j = -1; j <= 1; j++)
            {
                if (!inBounds(r+i, c+j))
                {continue;}

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
                if (!inBounds(r+i, c+j) || 
                    (i == 0 && j == 0))
                {continue;}

                if (isCellInfected(r+i, c+j))
                {a += 1;}

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
                if (!inBounds(r+i, c+j) || 
                    (i == 0 && j == 0))
                {continue;}

                if (isCellSick(r+i, c+j))
                {b += 1;}

            }
        }

        return b;
    }

    public boolean inBounds(int i, int j)
    {
        if (i < 0 || 
            i > grid.length-1 ||
            j < 0 ||
            j > grid[0].length-1)
        {
            return false;
        }

        return true;
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

    public void newMessage(String text, int timeout)
    {
        if (messages.size() > messageLimit)
        {return;}

        messages.add(0, text);
        messageTimeout.add(0, Integer.valueOf(timeout));
    }

    public static double clampDouble(double val, double min, double max) 
    {
        return Math.max(min, Math.min(max, val));
    }
    public static int clampInt(int  val, int  min, int max) 
    {
        return Math.max(min, Math.min(max, val));
    }

}