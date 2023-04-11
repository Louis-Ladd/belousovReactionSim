import java.awt.Graphics;
import java.awt.Color;

public class Cell
{
	private double n;
	private boolean isInfected;

	public Cell(double n)
	{
		this.n = n;
		this.isInfected = Application.rand.nextInt(0,2) == 1;
	}

	public double getN()
	{return n;}

	public void setN(double n)
	{this.n = n;}

	public boolean getIsInfected()
	{return isInfected;}

	public void setIsInfected(boolean isInfected)
	{this.isInfected = isInfected;}

	public void draw(Graphics g, int x, int y)
	{
		int cn = (int)clamp(n,0,255);
		g.setColor(new Color(cn, cn, cn));
		if (n <= 1)
		{
			g.setColor(Color.GREEN);
		}
		g.fillRect(x*16,y*16,12, 12);
	}

	public String toString()
	{
		return "" + n;
	}

	public static double clamp (double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

}