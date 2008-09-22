package bluectrl;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;


/**
 * Obsluga dzwiekow w midlecie
 * @author Kuba Odias
 * @version 1.0
 */
public class SoundPlayer implements Runnable {
	/**********************************VARIABLES**********************************/
	
	/** Obiekt klasy Player odpowiedzialny za zaladowanie i odtwarzanie dzwieku bledu */
	private Player playerError;
	
	/** Obiekt klasy Player odpowiedzialny za zaladowanie i odtwarzanie dzwieku zdarzenia (podczas zmiany stanu odtwarzacza muzycznego) */
	private Player playerPopup;	 
	
	/** Obiekt klasy Player odpowiedzialny za zaladowanie i odtwarzanie dzwieku klikniecia klawisza przez uzytkownika */
	private Player playerClick;	 
	
	/** Zmienna informujaca czy odtwarzacz dzwiekow jest wyciszony czy nie */
	private boolean isMuted;
	
	/*****************************************************************************/
	
	
	
	/**********************************CONSTANTS**********************************/
	
	/** Kod dzwieku wywolywanego w razie bledu */
	public static final int ERROR_SOUND=1;	
	
	/** Kod dzwieku odtwarzanego gdy uzytkownik nacisnal jakikolwiek klawisz */
	public static final int CLICK_SOUND=2;
	
	/** Kod dzwieku, ktory jest odtwarzany w momencie gdy jakakolwiek zmiana na komputerze (np. przelaczenie piosenki) spowoduje takze zmiane na komorce */
	public static final int POPUP_SOUND=3;
	
	/*****************************************************************************/
	
	
	
	/*******************************METHODS***************************************/
	
	/** Konstruktor klasy AudioPlayer
	 */
	public SoundPlayer() {		
		isMuted = true;
		new Thread(this).start();
	}

	/** Metoda uruchamiana przez watek, odpowiedzialna za zaladowanie i wstepne pobranie dzwiekow
	 */
	public void run() {
		try {
			InputStream isError = getClass().getResourceAsStream("/res/error.wav");
			InputStream isClick = getClass().getResourceAsStream("/res/click.wav");
			InputStream isPopup = getClass().getResourceAsStream("/res/popup.wav");
			playerError = Manager.createPlayer(isError, "audio/X-wav");
			playerError.prefetch();
			playerClick = Manager.createPlayer(isClick, "audio/X-wav");
			playerClick.prefetch();
			playerPopup = Manager.createPlayer(isPopup, "audio/X-wav");
			playerPopup.prefetch();
		} 
		catch(MediaException me) {
			System.out.println("Unknown media exception has occured.");
			me.printStackTrace();
		} 
		catch (IOException ioe) {
			System.out.println("File not found");
			ioe.printStackTrace();
		}	
	}
	
	/** Akcesor zmiennej isMuted
	 * @return <code>true</code> jesli dzwiek jest wyciszony, <code>false</code> w przeciwnym razie
	 */
	public boolean getIsMuted() {
		return isMuted;
	}

	/** Metoda odtwarzajaca podany dzwiek
	 * @param soundID	Kod dzwieku, ktory ma zostac odtworzony
	 */
	public void play(int soundID) {
		try {
			if(isMuted == false)	// jesli dzwieki nie sa wyciszone
				switch(soundID) {
					case ERROR_SOUND:
						playerError.start();
						break;
					case CLICK_SOUND:
						playerClick.start();
						break;
					case POPUP_SOUND:
						playerPopup.start();
						break;
					default:
						break;
				}
		} 
		catch(MediaException me) {
			System.out.println("Unknown media exception has occured.");
			me.printStackTrace();
		} 
	}
	
	/*****************************************************************************/
}
