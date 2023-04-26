public class Source
{
	//Entry point for the applictaion
	public static void main(String[] args) throws InterruptedException
	{
		System.out.println("WARNING: High parameter values can cause intense flashing.");
        System.out.println("Proceed with caution if you are prone to epileptic seizures.");

		Application window = new Application(false);
		window.setVisible(true);
	}
}