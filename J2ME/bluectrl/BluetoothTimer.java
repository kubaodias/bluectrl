package bluectrl;



import java.util.Timer;
import java.util.TimerTask;

/**
 * Klasa odpowiedzialna za odswiezanie czasu trwania utworu oraz sprawdzanie timeoutu w przypadku rozlaczenia urzadzen
 * @author Kuba Odias
 * @version 1.0
 */
public class BluetoothTimer extends TimerTask {
	/** Zmienna przechowujaca aktualny czas trwania utworu */
	private int currentTime;
	
	/** Obiekt odpowiedzialny za odliczanie czasu */
	private Timer timer = null;	

	/** Konstruktor klasy BluetoothTimer
	 */
	public BluetoothTimer() {
		currentTime = -1;
	}
	
	/** Metoda uruchamiana przez watek TimerTaska, odpowiedzialna za okresowe wykonywanie danych czynnosci
	 */
	public void run() {
		currentTime += 1;
	}
	
	/** Akcesor zmiennej currentTime
	 * @return 	Wartosc zmiennej currentTime
	 */
	public int getCurrentTime() {
		return currentTime;
	}
	
	/** Metoda ustawia wartosc zmiennej currentTime
	 * @param t 	Nowa wartosc zmiennej currentTime
	 */
	public void setCurrentTime(int t) {
		currentTime = t;
	}
	
	/** Metoda zwraca wartosc czasu trwania utworu w postaci tekstowej
	 * @return 	Wartosc czasu trwania utworu skonwertowany do stringa ze zmiennej currentTime
	 */
	public String getCurrentTimeString() {
		String ret = "";
		
		if(currentTime == -1)	// jesli timer jest wylaczony
			return "";
		
		ret = String.valueOf(currentTime / 60) + ":";
		
		if((currentTime % 60) < 10)
			ret +="0";
		
		ret += String.valueOf(currentTime % 60);
		
		return ret;
	}
	
	/** Metoda sluzaca do wlaczenia timera
	 */
	public void startTimer() {
		if(timer != null)
			timer.cancel();		// anulowanie wszystkich zadan, ktore mialyby sie wykonac
		
		timer = new Timer();
		timer.schedule(this, 1000, 1000);
	}
	
	/** Metoda sluzaca do zapauzowania timera
	 */
	public void pauseTimer() {
		try {
			if(timer != null)
				timer.cancel();		// anulowanie wszystkich zadan, ktore mialyby sie wykonac
		} 
		catch(IllegalStateException ise) { }
		
		timer = null;
	}

	/** Metoda sluzaca do zatrzymania timera
	 */
	public void stopTimer() {
		try {
			if(timer != null)
				timer.cancel();		// anulowanie wszystkich zadan, ktore mialyby sie wykonac
		} 
		catch(IllegalStateException ise) { }
		
		timer = null;
		currentTime = -1;
	}
	
	/** Metoda sprawdzajaca czy zegar jest uruchomiony
	 * @return	<code>true</code> jesli zegar jest uruchomiony, <code>false</code> w przeciwnym razie
	 */
	public boolean isRunning() {
		if(timer == null)
			return false;
		
		return true;
	}

}
