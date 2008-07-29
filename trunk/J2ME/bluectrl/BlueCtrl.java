package bluectrl;

import java.io.IOException;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.kxml.parser.*;
import org.kxml.kdom.*;
import org.kxml.*;



/** Glowna klasa aplikacji
 * @author Kuba Odias
 * @version 0.9
 */
public class BlueCtrl extends MIDlet implements CommandListener 
{

	/*******************************VARIABLES*************************************/
	
	/** Obiekt klasy Display odpowiedzialny za wyglad aplikacji */
	Display myDisplay;
	
	/** Plotno - obiekt klasy Canvas, do ktorego dodawane sa np. przyciski */
	MainCanvas mainCanvas;
	
	/** Komenda odpowiedzialna za wyjscie z aplikacji */
	private Command exitCommand = new Command("Exit", Command.SCREEN, 1);
	
	/** Komenda odpowiedzialna za wyjscie z aplikacji */
	private Command mediaLibraryCommand = new Command("Media Library", Command.SCREEN, 1);
	
	/** Komenda odpowiedzialna za powrot do glownego ekranu aplikacji */
	private Command backCommand = new Command("Back", Command.SCREEN, 1);
	
	/** Komenda odpowiedzialna za powrot do glownego ekranu aplikacji */
	private Command reconnectCommand = new Command("Reconnect", Command.SCREEN, 1);
	
	/** Zmienna informujaca czy aplikacja zostala juz uruchomiona (aby nie wywolywac wielokrotnie metody startApp) */
	private boolean isApplicationStarted = false;
	
	/*****************************************************************************/
	
	
	/*******************************METHODS***************************************/
	
	/** Konstruktor obiektu klasy BlueCtrl 
	 * @author Kuba Odias
	 * @version 1.0
	 */
	public BlueCtrl() 
	{
		myDisplay = Display.getDisplay(this);
	}
	
	/** Metoda destroyApp z cyklu zycia MIDletu - wywolywana przy zamykaniu aplikacji
	 * @author Kuba Odias
	 * @version 1.0
	 * @throws MIDletStateChangeException
	 */
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException 
	{
		mainCanvas.destroy();
	}

	/** Metoda pauseApp z cyklu zycia MIDletu - wywolywana np. przy polaczeniach przychodzacych
	 * @author Kuba Odias
	 * @version 1.0
	 */
	protected void pauseApp() 
	{
		mainCanvas.destroy();
	}

	/** Metoda startApp z cyklu zycia MIDletu - wywolywana przy zamykaniu aplikacji
	 * @author Kuba Odias
	 * @version 1.0
	 * @throws MIDletStateChangeException
	 */
	protected void startApp() throws MIDletStateChangeException 
	{
		if(!isApplicationStarted)	// blok wywolywany tylko przy pierwszym starcie MIDletu
		{	
			isApplicationStarted = true;
			try 
			{
				mainCanvas = new MainCanvas();
			} 
			catch (IOException e) 
			{
				SoundPlayer soundPlayer = new SoundPlayer();
				new Thread(soundPlayer).start();
				soundPlayer.play(SoundPlayer.ERROR_SOUND);
				
				System.out.println("Blad podczas wczytywania pliku z zasobow!");
				e.printStackTrace();
			}

			mainCanvas.addCommand(mediaLibraryCommand);
			mainCanvas.addCommand(reconnectCommand);
			mainCanvas.addCommand(exitCommand);
			mainCanvas.setCommandListener(this);
			myDisplay.setCurrent(mainCanvas);
			
	        mainCanvas.repaint();
		}
	}
	
	/** Metoda wywolywana przez CommandListener w momencie nacisniecia ktoregos z przyciskow
	 * @author Kuba Odias
	 * @version 1.0
	 */
	public void commandAction(Command cmd, Displayable arg1) 
	{
		if (cmd == exitCommand) 
		{
            try 
            {
                destroyApp(false);
                notifyDestroyed();
                mainCanvas.destroy();
            } 
            catch(Exception e) 
            {
            	SoundPlayer soundPlayer = new SoundPlayer();
				new Thread(soundPlayer).start();
				soundPlayer.play(SoundPlayer.ERROR_SOUND);
				
                e.printStackTrace();
            }
        }
		else if(cmd == mediaLibraryCommand)
		{
			removeAllCommands(mainCanvas);
			
			mainCanvas.addCommand(backCommand);
			mainCanvas.addCommand(exitCommand);
			mainCanvas.setDisplayedScreen(MainCanvas.MEDIA_LIBRARY_SCREEN);
		}
		else if(cmd == backCommand)
		{
			removeAllCommands(mainCanvas);
			mainCanvas.addCommand(mediaLibraryCommand);
			mainCanvas.addCommand(reconnectCommand);
			mainCanvas.addCommand(exitCommand);
			mainCanvas.setDisplayedScreen(MainCanvas.PLAYER_SCREEN);
		}
		else if(cmd == reconnectCommand)
		{
			mainCanvas.reconnectToServer();
		}
	}
	
	/** Metoda usuwa wszystkie komendy przypisane do plotna
	 * @author Kuba Odias
	 * @version 1.0
	 * @param c		Plotno, z ktorego zostana usuniete wszystkie elementy
	 */
	public void removeAllCommands(GameCanvas c)
	{
		mainCanvas.removeCommand(mediaLibraryCommand);
		mainCanvas.removeCommand(reconnectCommand);
		mainCanvas.removeCommand(backCommand);
		mainCanvas.removeCommand(exitCommand);
	}

	/****************************************************************************/
}
