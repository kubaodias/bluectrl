package bluectrl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;


/**
 * Klasa przechowujaca informacje o odtwarzaczu muzycznym
 * @author Kuba Odias
 * @version 0.7
 */
public class BluetoothPlayer implements Runnable, DiscoveryListener
{
	/*******************************VARIABLES*************************************/
	
	/** Urzadzenie Bluetooth znajdujace sie w telefonie */
	private LocalDevice localDevice;
	
	/** Lokalny agnet sluzacy do wyszukiwania urzadzen i serwisow */
	private DiscoveryAgent agent;
	
	/** Adres urzadzenia z ktorym sie laczymy */
	private String remoteDeviceBluetoothAddress = "";
	
	/** Nazwa urzadzenia z ktorym sie laczymy */
	private String remoteDeviceFriendlyName = "";
	
	/** Adres urzadzenia za pomoca ktorego sie laczymy (Bluetooth w telefonie) */
	private String localDeviceBluetoothAddress = "";
	
	/** Nazwa urzadzenia za ktorego pomoca sie laczymy (Bluetooth w telefonie) */
	private String localDeviceFriendlyName = "";
	
	/** Obiekt umozliwiajacy polaczenie miedzy dwoma urzadzeniami posiadajacymi Bluetooth */
	private StreamConnection bluetoothConnection = null;
	
	/** Zmienna typu bool informujaca czy udalo sie nawiazac polaczenie poprzez Bluetooth */
	private boolean isConnectedToServer;
	
	/** Zmienna informujaca czy wystapil blad spowodowany przez Bluetooth */
	private boolean bluetoothError;
	
	/** Zmienna informujaca czy odpytywanie urzadzen zostalo juz zakonczone */
	private boolean inquiryStarted;
		
	/** Wektor zapamietujacy nowo znalezione urzadzenia */
	private Vector discoveredDevices = new Vector();
	
	/** Wektor zapamietujacy identyfikatory uzywane podczas wyszukiwania serwisow */
	private Vector devicesTransIDs = new Vector();
	
	/** Strumien wyjsciowy sluzacy do wysylania danych przez lacze Bluetooth */
	private OutputStream out;
	
	/** Strumien wejsciowy sluzacy do odbierania danych przez lacze Bluetooth */
	private InputStream in;
	
	/** Wektor przechowujacy wywolane przez uzytkownika komendy */
	private Vector commandVector;
	
	/** Tytul utworu, ktory jest w danej chwili odtwarzany */
	private String title = null;
	
	/** Nazwa artysty, ktorego utwor jest w danej chwili odtwarzany */
	private String artist = null;
	
	/** Calkowity czas trwania utworu */
	private int totalTime = 0;
	
	/** Glosnosc dzwieku na komputerze */
	private int volumeLevel;
	
	/** Czas przez ktory urzadzenie moze odpoczywac - zalezny od czestotliwosci wysylania i odbierania danych */
	private int sleepTime;

	/** Stan odtwarzania utworu zwrocony przez serwer: odtwarzanie - PLAY, pauza - PAUSE, zatrzymanie - STOP */
	private String playingState;
	
	/** Zmienna informujaca o zmianie stanu odtwarzacza na komputerze */
	private boolean stateChanged;
	
	/** Zmienna informujaca o tym, ze stan odtwarzacza zostal zmieniony po stronie serwera, a nie urzadzenia mobilnego */
	private boolean stateChangedByServer;
	
	/** Obiekt klasy odpowiedzialnej za uplyw czasu w odtwarzanym utworze oraz sprawdzanie timeoutu podczas utraty polaczenia przez Bluetooth */
	private BluetoothTimer bluetoothTimer;
	
	/** Obiekt klasy MediaLibrary pozwalajacy na zaladowanie oraz obsluge biblioteki muzycznej */
	private MediaLibrary mediaLibrary;
	
	/*****************************************************************************/
	
	
	/*******************************CONSTANTS*************************************/
	
	/** Nazwa serwisu btspp */
	private static final String BLUE_CTRL_SERVICE_NAME = "BlueCtrl Server";
	
	/** Komenda wywolywana gdy uzytkownik chce wlaczyc utwor */
	public static final String COMMAND_PLAY = "COMMAND_PLAY";
	
	/** Komenda wywolywana gdy uzytkownik chce zapauzowac utwor */
	public static final String COMMAND_PAUSE = "COMMAND_PAUSE";
	
	/** Komenda wywolywana gdy uzytkownik chce zatrzymac utwor */
	public static final String COMMAND_STOP = "COMMAND_STOP";
	
	/** Komenda wywolywana gdy uzytkownik chce wlaczyc nastepny utwor */
	public static final String COMMAND_NEXT = "COMMAND_NEXT";
	
	/** Komenda wywolywana gdy uzytkownik chce wlaczyc poprzedni utwor */
	public static final String COMMAND_PREV = "COMMAND_PREV";
	
	/** Komenda wywolywana gdy uzytkownik chce zwiekszyc glosnosc utworu */
	public static final String COMMAND_VOLUME_UP = "COMMAND_VOLUME_UP";
	
	/** Komenda wywolywana gdy uzytkownik chce zmniejszyc glosnosc utworu */
	public static final String COMMAND_VOLUME_DOWN = "COMMAND_VOLUME_DOWN";
	
	/** Komenda wywolywana gdy uzytkownik chce pobrac nazwe utworu */
	public static final String COMMAND_GET_TITLE = "COMMAND_GET_TITLE";
	
	/** Komenda wywolywana gdy uzytkownik chce pobrac nazwe wykonawcy utworu */
	public static final String COMMAND_GET_ARTIST = "COMMAND_GET_ARTIST";
	
	/** Komenda wywolywana gdy uzytkownik chce pobrac calkowity czas trwania utworu */
	public static final String COMMAND_GET_TOTAL_TIME = "COMMAND_GET_TOTAL_TIME";
	
	/** Komenda wywolywana gdy uzytkownik chce pobrac aktualny czas trwania utworu */
	public static final String COMMAND_GET_CURRENT_TIME = "COMMAND_GET_CURRENT_TIME";
	
	/** Komenda wywolywana gdy uzytkownik chce pobrac glosnosc dzwieku odtwarzacza */
	public static final String COMMAND_GET_VOLUME_LEVEL = "COMMAND_GET_VOLUME_LEVEL";
	
	/** Komenda wywolywana gdy zmienil sie tan odtwarzacza na komputerze - np. utwor zostal zapauzowany lub wznowiony */
	public static final String COMMAND_CHANGE_STATE = "COMMAND_CHANGE_STATE";

	/** Komenda wywolywana gdy uzytkownik chce pobrac biblioteke muzyczna */
	public static final String COMMAND_GET_MEDIA_LIBRARY = "COMMAND_GET_MEDIA_LIBRARY";
	
	/*****************************************************************************/

	
	/*******************************METHODS***************************************/
	
	/** Konstruktor klasy Player 
	 * @author Kuba Odias
	 * @version 1.0
	 */
	public BluetoothPlayer()
	{
		bluetoothError = false;
		isConnectedToServer = false;
		inquiryStarted = false;
		commandVector = new Vector();
		sleepTime = 200;
		bluetoothTimer = new BluetoothTimer();
		mediaLibrary = new MediaLibrary();

		//mediaLibrary.parsePlaylist(this.getClass().getResourceAsStream("/res/playlist.xml"));
		new Thread(this).start();
	}
	
	/** Akcesor zmiennej isConnectedToServer
	 * @author Kuba Odias
	 * @version 1.0 
	 * @return
	 */
	public boolean getIsConnectedToServer()
	{
		return isConnectedToServer;
	}
	
	/** Akcesor zmiennej remoteDeviceBluetoothAddress 
	 * @author Kuba Odias
	 * @version 1.0 
	 * @return	Adres urzadzenia z ktorym sie laczymy
	 */
	public String getRemoteDeviceBluetoothAddress()
	{
		return remoteDeviceBluetoothAddress;
	}
	
	/** Akcesor zmiennej remoteDeviceFriendlyName
	 * @author Kuba Odias
	 * @version 1.0 
	 * @return	Nazwa urzadzenia z ktorym sie laczymy
	 */
	public String getRemoteDeviceFriendlyName()
	{
		return remoteDeviceFriendlyName;
	}
	
	/** Akcesor zmiennej remoteDeviceBluetoothAddress 
	 * @author Kuba Odias
	 * @version 1.0 
	 * @return	Adres urzadzenia za pomoca ktorego sie laczymy (Bluetooth w telefonie)
	 */
	public String getLocalDeviceBluetoothAddress()
	{
		return localDeviceBluetoothAddress;
	}
	
	/** Akcesor zmiennej remoteDeviceFriendlyName
	 * @author Kuba Odias
	 * @version 1.0 
	 * @return	Nazwa urzadzenia za pomoca ktorego sie laczymy (Bluetooth w telefonie)
	 */
	public String getLocalDeviceFriendlyName()
	{
		return localDeviceFriendlyName;
	}
	
	/** Akcesor zmiennej bluetoothError
	 * @author Kuba Odias
	 * @version 1.0  
	 * @return	Wartosc zmiennej informujacej czy wystapil blad spowodowany przez Bluetooth
	 */
	public boolean getBluetoothError()
	{
		return bluetoothError;
	}
	
	/** Ustawia wartosc zmiennej bluetoothError
	 * @author Kuba Odias
	 * @version 1.0  
	 * @return	Nowa wartosc zmiennej informujacej czy wystapil blad spowodowany przez Bluetooth
	 */
	public void setBluetoothError(boolean b)
	{
		bluetoothError = b;
	}
	
	/** Akcesor zmiennej inquiryStarted 
	 * @author Kuba Odias
	 * @version 1.0
	 * @return	Zmienna informujaca czy odpytywanie urzadzen zostalo juz zakonczone 
	 */
	public boolean getInquiryStarted()
	{
		return inquiryStarted;
	}
	
	/** Akcesor zmiennej title
	 * @author Kuba Odias
	 * @version 1.0
	 * @return	Wartosc zmiennej przechowujacej tytul odtwarzanego utworu 
	 */
	public String getTitle()
	{
		return title;
	}
	
	/** Akcesor zmiennej artist
	 * @author Kuba Odias
	 * @version 1.0
	 * @return	Wartosc zmiennej przechowujacej nazwe artysty, ktorego utwor jest w danej chwili odtwarzany
	 */
	public String getArtist()
	{
		return artist;
	}
	
	/** Akcesor zmiennej totalTime
	 * @author Kuba Odias
	 * @version 1.0
	 * @return	Wartosc zmiennej przechowujacej calkowity czas trwania utworu
	 */
	public int getTotalTime()
	{
		return totalTime;
	}
	
	/** Akcesor zmiennej currentTime z klasy BluetoothTimer
	 * @author Kuba Odias
	 * @version 1.0
	 * @return	Wartosc zmiennej przechowujacej aktualny czas trwania utworu
	 */
	public int getCurrentTime()
	{
		return bluetoothTimer.getCurrentTime();
	}
	
	/** Akcesor zmiennej currentTimeString
	 * @author Kuba Odias
	 * @version 1.0
	 * @return	Wartosc zmiennej przechowujacej aktualny czas trwania utworu
	 */
	public String getCurrentTimeString()
	{
		return bluetoothTimer.getCurrentTimeString();
	}
	
	/** Akcesor zmiennej volumeLevel
	 * @author Kuba Odias
	 * @version 1.0
	 * @return	Wartosc zmiennej przechowujacej glosnosc dzwieku
	 */
	public int getVolumeLevel()
	{
		return volumeLevel;
	}
	
	/** Akcesor zmiennej playingState
	 * @author Kuba Odias
	 * @version 1.0
	 * @return	Wartosc zmiennej przechowujacej stan odtwarzacza
	 */
	public String getPlayingState()
	{
		String state = playingState;
		playingState = null;
		
		return state;
	}
	
	/** Akcesor zmiennej stateChanged
	 * @author Kuba Odias
	 * @version 1.0
	 * @return	Wartosc zmiennej informujacej o zmianie stanu odtwarzacza
	 */
	public boolean getStateChanged()
	{
		boolean state = stateChanged;
		stateChanged = false;
		
		return state;
	}
	
	/** Akcesor zmiennej bluetoothTimer
	 * @author		Kuba Odias
	 * @version	1.0
	 * @return		Wartosc zmiennej bluetoothTimer
	 */
	public BluetoothTimer getBluetoothTimer()
	{
		return bluetoothTimer;
	}
	
	/** Akcesor zmiennej mediaLibrary
	 * @author		Kuba Odias
	 * @version	1.0
	 * @return		Wartosc zmiennej mediaLibrary
	 */
	public MediaLibrary getMediaLibrary()
	{
		return mediaLibrary;
	}
	
	/** Akcesor zmiennej mediaLibraryDownloadedBytes
	 * @author		Kuba Odias
	 * @version	1.0
	 * @return		Wartosc zmiennej mediaLibraryDownloadedBytes
	 */
	public int getMediaLibraryDownloadedBytes() {
		return mediaLibrary.getMediaLibraryDownloadedBytes();
	}
	
	/** Akcesor zmiennej mediaLibrarySize
	 * @author		Kuba Odias
	 * @version	1.0
	 * @return		Wartosc zmiennej mediaLibrarySizes
	 */
	public int getMediaLibrarySize() {
		return mediaLibrary.getMediaLibrarySize();
	}

	/** Metoda dodaje nowa komende do wektora
	 * @author Kuba Odias
	 * @version 1.0
	 * @param	Identyfikator nowej komendy
	 */
	public void addCommandToProcess(String cmd)
	{
		commandVector.addElement(cmd);
	}
	
	/** Metoda uruchamiana przez watek, odpowiedzialna za zainicjalizowanie polaczenia z komputerem
	 * @author Kuba Odias
	 * @version 0.7
	 */
	public void run() 
	{	
		try 
		{
			localDevice = LocalDevice.getLocalDevice();
			agent = localDevice.getDiscoveryAgent();
		} 
		catch (BluetoothStateException e) 
		{
			closeConnection();
			e.printStackTrace();
		}

		while (isBluetoothOn())
		{	
			findBlueCtrlServer();
			
			while (inquiryStarted)	// czekanie dopoki trwa wyszukiwanie urzadzen i serwisow
			{
				try 	// uspienie watku na 200 milisekund
				{
					Thread.sleep(100);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
			
			while (isConnectedToServer)	// glowna petla przetwarzajaca dane
			{
				if (sleepTime < 100)
					sleepTime+=10;
				else if(sleepTime < 200)
					sleepTime+=20;
				receiveCommands();
				
				sendCommands();	// jesli sprawdzono czy istnieja dane do odebrania to bycmoze istnieja jakies dane do wyslania
				
				try 	// uspienie watku na czas sleepTime
				{
					Thread.sleep(sleepTime);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
			
			try 	// uspienie watku na 4 sekundy w przypadku rozlaczenia
			{
				Thread.sleep(4000);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		
			closeConnection();
		}
	}

	/** Metoda sprawdzajaca czy Bluetooth jest wlaczony
	 * @author 			Kuba Odias
	 * @version 		1.0
	 * @return	<code>true</code> jesli Bluetooth jest wlaczony, <code>false</code> w przeciwnym razie
	 */
	public boolean isBluetoothOn()
	{
		if(localDevice == null)
		{
			try 
			{
				localDevice = LocalDevice.getLocalDevice();
			} 
			catch (BluetoothStateException e) 
			{
				return false;
			}
		}
		
		if(localDevice.getBluetoothAddress().equals(""))
		{
			closeConnection();
			return false;
		}

		localDevice = null;
		
		return true;
	}

	/** Metoda odpowiedzialna za wyszukanie i nawiazanie polaczenia z serwerem
	 * @author Kuba Odias
	 * @version 0.8
	 */
	public void findBlueCtrlServer()
	{
		try 
		{				
			bluetoothError = false;
			playingState = null;
			stateChangedByServer = true;
			stateChanged = false;

			/* Wyczyszczenie wektorow przechowujacych informacje o urzadzeniach, serwisach i komendach, ktore maja zostac wyslane */
			discoveredDevices.removeAllElements();
			devicesTransIDs.removeAllElements();
			commandVector.removeAllElements();
		
			localDevice = LocalDevice.getLocalDevice();
			agent = localDevice.getDiscoveryAgent();
	
			inquiryStarted = agent.startInquiry(DiscoveryAgent.GIAC, this);
		} 
		catch (BluetoothStateException e) 
		{
			inquiryStarted = false;
			closeConnection();
		}
	}
	
	/** Metoda zamykajaca polaczenie z urzadzeniem Bluetooth 
	 * @author Kuba Odias
	 * @version 0.7
	 */
	public void closeConnection()
	{
		isConnectedToServer = false;
		bluetoothError = true;
		inquiryStarted = false;
		try
		{
			if(bluetoothConnection != null)
			{
				bluetoothConnection.close();
				bluetoothConnection = null;
			}
			
			if(out != null)
			{
				out.close();
				out = null;
			}
			
			if(in != null)
			{
				in.close();
				in = null;
			}
			
			if(agent != null)
			{
				agent.cancelInquiry(this);
				agent = null;
			}
			
			localDevice = null;
		} 
		catch (IOException e) 
		{
			bluetoothConnection = null;
		}
	}

	/** Metoda wywolywana przez DiscoveryListener w momencie znalezienia nowego urzadzenia
	 * @author Kuba Odias
	 * @version 0.9
	 * @param remoteDevice	Znalezione urzadzenie
	 * @param deviceClass	Klasa znalezionego urzadzenia
	 */
	public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass deviceClass) 
	{      
		discoveredDevices.addElement(remoteDevice);
	}

	/** Metoda wywolywana przez DiscoveryListener w momencie gdy zakonczone zostalo wyszukiwanie nowych urzadzen
	 * @author Kuba Odias
	 * @version 0.9
	 * @param type		Typ zadania, ktore zostalo wykonane: INQUIRY_COMPLETED, INQUIRY_TERMINATED lub INQUIRY_ERROR     
	 */
	public void inquiryCompleted(int type)
	{
        UUID[] uuidSet = new UUID[1];
        uuidSet[0] = new UUID(0x1101);	// tylko serwisy btspp
        int attributes[] = {0x100};
       
        if (type == DiscoveryListener.INQUIRY_COMPLETED)
        {
	        int n = discoveredDevices.size();

	        if(n > 0)
	        {
	            for (int i = 0; i < n; i++)
	            {
	            	RemoteDevice rd = (RemoteDevice) discoveredDevices.elementAt(i);
	                
                	try
                	{
                		int transID = agent.searchServices(attributes, uuidSet, rd, this);
                		devicesTransIDs.addElement(new Integer(transID));	// musi byc dodawany obiekt klasy Object
                	}
                	catch (BluetoothStateException e1) 
                    {
     					e1.printStackTrace();
     				}
                }
	        } 
	        else	// nie znaleziono zadnych urzadzen
	        {
	        	inquiryStarted = false;
	        	bluetoothError = true;
	        }
        }
        else
        {
        	inquiryStarted = false;
        	bluetoothError = true;
        }
	}

	/** Metoda wymagana przez interfejs DiscoveryListener
	 * @author Kuba Odias
	 * @version 1.0
	 * @param transID		Identyfikator wyszukiwania serwisow
	 * @param responseCode	Kod zwrocony przez DiscoveryListener
	 */
	public void serviceSearchCompleted(int transID, int responseCode) 
	{
		devicesTransIDs.removeElement(new Integer(transID));
		
		if(devicesTransIDs.size() == 0)
		{
			inquiryStarted = false;	// zakonczono wyszukiwanie serwisow
			
			if (bluetoothConnection == null)	// jesli klient nie polaczyl sie z serwisem BlueCtrl
			{
				bluetoothError = true;
			}
		}
	}

	/** Metoda wymagana przez interfejs DiscoveryListener - wywoływana w momencie znalezienia nowych serwisów
	 * @author Kuba Odias
	 * @version 1.0
	 * @param transID	Identyfikator wyszukiwania serwisow
	 * @param sr		Tablica zawierajaca znalezione serwisy
	 */
	public void servicesDiscovered(int transID, ServiceRecord[] sr) 
	{
		String bluetoothURLString;

		for(int i = 0; (i < sr.length) && (isConnectedToServer == false); i++)
		{
			DataElement elem = sr[i].getAttributeValue(0x100);
		
	        if (elem != null)
	        {        
            	if ((elem.getDataType() == DataElement.STRING) && (elem.getValue().toString().compareTo(BLUE_CTRL_SERVICE_NAME) == 0))
            	{	
    				bluetoothURLString = sr[i].getConnectionURL(ServiceRecord.AUTHENTICATE_NOENCRYPT, false);
    				
    				if(authenticateServer(bluetoothURLString) == false)		// proba autentykacji urzadzenia
    				{
    					try
    					{
    						if(bluetoothConnection != null)
    						{
    							bluetoothConnection.close();
    							bluetoothConnection = null;
    						}
    						
    						if(out != null)
    						{
    							out.close();
    							out = null;
    						}
    						
    						if(in != null)
    						{
    							in.close();
    							in = null;
    						}
    					}
    					catch (IOException e) 
    					{
    						bluetoothConnection = null;
    						out = null;
    					}
    				}
            	}
	        }
		}
}
	
	/** Funkcja sluzaca do wyslania danych przez lacze Bluetooth
	 * @author Kuba Odias
	 * @version 0.8
	 * @param data	Dane, ktore maja zostac wyslane
	 * @return		<code>true</code> jesli wyslanie danych powiodlo sie, <code>false</code> w przeciwnym razie
	 */
	public boolean bluetoothSendData(String data)
	{
		String ack;
		
		if(bluetoothConnection != null)
		{
			try 
			{	
				out.write(data.length());
				out.write(data.getBytes());
				out.flush();

				if (sleepTime > 100)
					sleepTime=100;
				else if(sleepTime > 20)
					sleepTime-=10;
				ack = bluetoothReceiveAcknowledge();
				if(ack == null)
					return false;
				else if(ack.equals("ACK"))
					return true;

				return false;
			} 
			catch (IOException e) 
			{
				closeConnection();
				return false;
			}
		}
		else
			return false;
	}
	
	/** Funkcja sluzaca do wyslania potwierdzenia odbioru danych
	 * @author Kuba Odias
	 * @version 1.0
	 * @param data	Potwierdzenie, ktore ma zostac wyslane
	 * @return		<code>true</code> jesli wyslanie danych powiodlo sie, <code>false</code> w przeciwnym razie
	 */
	public boolean bluetoothSendAcknowledge(String data)
	{
		if(bluetoothConnection != null)
		{
			try 
			{	
				out.write(data.length());
				out.write(data.getBytes());
				out.flush();
				if (sleepTime > 100)
					sleepTime=100;
				else if(sleepTime > 20)
					sleepTime-=10;
			} 
			catch (IOException e) 
			{
				closeConnection();
				return false;
			}
		}
		else
			return false;
		
		return true;
	}
	
	/** Funkcja sluzaca do odebrania danych przez lacze Bluetooth
	 * @author Kuba Odias
	 * @version 0.8
	 * @return	Odebrane dane, lub null w razie bledu 
	 */
	public String bluetoothReceiveData()
	{
		String ret = null;
		int length = -1;
		
		try 
		{		
			length = in.read();
			
	        if (length <= 0) 
	        {
	        	bluetoothSendAcknowledge("ERROR");
	        	return null;
	        }
	        
	        byte[] input = new byte[length];
	        length = 0;

	        while (length != input.length) 
	        {
	            int n = in.read(input, length, input.length - length);
	            
	            if(n == -1) 
	            {
	            	bluetoothSendAcknowledge("ERROR");
	            	return null;
	            }
	            length += n;
	        }
	        
	        ret = new String(input);
	        
	        if(ret.equals("ERROR"))
	        	bluetoothSendAcknowledge("ERROR");
	        else
	        	bluetoothSendAcknowledge("ACK");
	        
	        if (sleepTime > 100)
				sleepTime=100;
			else if(sleepTime > 20)
				sleepTime-=10;
		} 
		catch (IOException e) 
		{
			bluetoothSendAcknowledge("ERROR");
			closeConnection();
			return null;
		}
		
        return ret;
	}
	
	
	/** Funkcja sluzaca do odebrania potwierdzenia przeslania danych przez lacze Bluetooth
	 * @author Kuba Odias
	 * @version 0.8
	 * @return	Odebrane potwierdzenie, lub null w razie bledu 
	 */
	public String bluetoothReceiveAcknowledge()
	{
		String ret = null;
		int length = -1;
		
		try 
		{		
			while(in.available() == 0) {}
			length = in.read();

	        if (length <= 0) 
	        {
	        	return null;
	        }
	        
	        byte[] input = new byte[length];
	        length = 0;

	        while (length != input.length) 
	        {
	            int n = in.read(input, length, input.length - length);
	            
	            if(n == -1) 
	            {
	            	return null;
	            }
	            length += n;
	        }
	     
	        ret = new String(input);
	        
	        if (sleepTime > 100)
				sleepTime=100;
			else if(sleepTime > 20)
				sleepTime-=10;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return null;
		}
		
        return ret;
	}
	
	
	/** Metoda sluzaca do sprawdzenia czy mozliwe jest polaczenie i autoryzowanie danego serwera BlueCtrl
	 * @author 	Kuba Odias
	 * @version 	0.4
	 * @param 		urlString Link URL, z ktory sluzy do polaczenia dwoch urzadzen
	 * @return 	<code>true</code> jesli autoryzacja powiodla sie, <code>false</code> w przeciwnym razie
	 */
	public boolean authenticateServer(String urlString)
	{
		
		if (urlString != null) 
		{
			try 
			{
				bluetoothConnection = (StreamConnection) Connector.open(urlString); // polaczenie z serwerem
				
				if(bluetoothConnection != null)
				{
					
					out = bluetoothConnection.openOutputStream();
					in = bluetoothConnection.openInputStream();
					
					if(bluetoothSendData("CONNECT") == false)
						return false;
	                
	        		String msg = bluetoothReceiveData();
					if((msg != null) && (msg.equals("CONNECT_ACK")))
					{
						isConnectedToServer = true;
					}
					else
						closeConnection();
				}
				else
				{
					return false;
				}
			} 
			catch (IOException e) 
			{
				return false;
			}
		} 
		else 
		{
			return false;
		}
		return true;
	}
	
	
	/** Metoda sluzaca do wykonania czynnosci zgloszonych przez uzytkownika telefonu
	 * @author Kuba Odias
	 * @version 0.9
	 */
	public void sendCommands() {
		while (!commandVector.isEmpty()) {
			String cmd = (String) commandVector.firstElement();	// pobiera najdawniej wywolana komende
			
			if(cmd != null) {
				commandVector.removeElementAt(0);	// usuwa pobrany element
				if(bluetoothSendData(cmd) == false) {		// i wysyla komende przez Bluetooth
					commandVector.insertElementAt(cmd, 0);	// ponowne ustawienie elementu do wyslania
					return;		// ponowna proba wyslania danych nastapi po chwili
				}
				else {
					// jesli uzytkownik wyslal ktorakolwiek z ponizszych komend to znaczy ze stan zostal zmieniony przez urzadzenie mobilne
					if ((cmd.equals(COMMAND_PLAY)) || (cmd.equals(COMMAND_PAUSE)) || (cmd.equals(COMMAND_NEXT)) || 
							(cmd.equals(COMMAND_PREV)) || (cmd.equals(COMMAND_STOP)) || 
								(cmd.equals(COMMAND_VOLUME_DOWN)) || (cmd.equals(COMMAND_VOLUME_UP)))
						stateChangedByServer = false;
				}
				
				receiveCommands();
			}
			
		}
	}
	
	/** Metoda sluzaca do odebrania od komputera ewentualnie wystepujacych danych
	 * @author Kuba Odias
	 * @version 0.7
	 */
	public void receiveCommands() {
		String msg = null;
		try {
			while(in.available() != 0) {		// jesli sa jakies dane do odebrania
				msg = bluetoothReceiveData();
				processReceivedCommand(msg);	// przetworzenie otrzymanej wiadomosci
			}
		} 
		catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	/** Metoda sluzaca do rozpoczecia odtwarzania utworu
	 * @author Kuba Odias
	 * @version 0.9
	 */
	public void play() {
		addCommandToProcess(BluetoothPlayer.COMMAND_PLAY);
	}
	
	/** Metoda sluzaca do zapauzowania utworu
	 * @author Kuba Odias
	 * @version 0.9
	 */
	public void pause() {
		addCommandToProcess(BluetoothPlayer.COMMAND_PAUSE);
	}
	
	/** Metoda sluzaca do zatrzymania utworu
	 * @author Kuba Odias
	 * @version 0.9
	 */
	public void stop() {
		addCommandToProcess(BluetoothPlayer.COMMAND_STOP);
	}
	
	/** Metoda sluzaca do zwiekszenia glosnosci utworu
	 * @author Kuba Odias
	 * @version 1.0
	 */
	public void volumeUp() {
		addCommandToProcess(BluetoothPlayer.COMMAND_VOLUME_UP);
	}
	
	/** Metoda sluzaca do zwiekszenia glosnosci utworu
	 * @author Kuba Odias
	 * @version 1.0
	 */
	public void volumeDown() {
		addCommandToProcess(BluetoothPlayer.COMMAND_VOLUME_DOWN);
	}
	
	
	/** Metoda sluzaca do wykonania czynnosci zwiazanych z otrzymana komenda
	 * @author Kuba Odias
	 * @version 0.5
	 * @param msg	Otrzymana wiadomosc
	 */
	public void processReceivedCommand(String msg) {
		if(msg.equals(COMMAND_GET_TITLE)) {
			title = bluetoothReceiveData();
			
			if(stateChangedByServer == true)
				stateChanged = true;
			
			stateChangedByServer = true;
		}
		else if(msg.equals(COMMAND_GET_ARTIST)) {
			artist = bluetoothReceiveData();
		}
		else if(msg.equals(COMMAND_GET_TOTAL_TIME)) {
			String data = bluetoothReceiveData();
			int index = data.indexOf(":");
			
			if(index == -1)
				totalTime = 0;
			else {
				totalTime = 60*Integer.parseInt(data.substring(0, index));
				totalTime += Integer.parseInt(data.substring(index + 1, data.length()));
			}
		}
		else if(msg.equals(COMMAND_GET_CURRENT_TIME)) {
			int currentTime;
			String currentTimeString = bluetoothReceiveData();
			
			int index = currentTimeString.indexOf(":");
			if(index == -1) {
					currentTime = 0;
			}
			else {
				currentTime = 60*Integer.parseInt(currentTimeString.substring(0, index));
				currentTime += Integer.parseInt(currentTimeString.substring(index + 1, currentTimeString.length()));
			}
			bluetoothTimer.setCurrentTime(currentTime);
		}
		else if(msg.equals(COMMAND_GET_VOLUME_LEVEL)) {
			volumeLevel = Integer.parseInt(bluetoothReceiveData());
			
			if(stateChangedByServer == true)
				stateChanged = true;
			
			stateChangedByServer = true;
		}
		else if(msg.equals(COMMAND_CHANGE_STATE)) {
			playingState = bluetoothReceiveData();
			
			if(playingState.equals("PLAY")) {
				int t;
				
				if((t = bluetoothTimer.getCurrentTime()) != -1) {
					if(bluetoothTimer.isRunning() == true)
						bluetoothTimer.setCurrentTime(0);
					else
						bluetoothTimer.setCurrentTime(t);
				}
				else
					bluetoothTimer.setCurrentTime(0);
				
				if(bluetoothTimer.isRunning() == false)
					bluetoothTimer.startTimer();
			}
			if(playingState.equals("OTHER")) {				
				bluetoothTimer.setCurrentTime(0);
				
				if(bluetoothTimer.isRunning() == false)
					bluetoothTimer.startTimer();
			}
			else if(playingState.equals("PAUSE")) {			
				bluetoothTimer.pauseTimer();
			}
			else if(playingState.equals("STOP")) {
				bluetoothTimer.stopTimer();
			}			
		}
		else if(msg.equals(COMMAND_GET_MEDIA_LIBRARY)) {
			mediaLibrary.setMediaLibrarySize(Integer.parseInt(bluetoothReceiveData()));
			//downloadMediaLibrary();
			mediaLibrary.parsePlaylist(in);
		}

	}
	
	/** Metoda wywolujaca metode parsePlaylist z klasy MediaLibrary z dodatkowym argumentem - strumieniem z ktorego bedzie czytany plik
	 * @author Kuba Odias
	 * @version 0.6
	 */
	public void getPlaylist() {
		if (isConnectedToServer == true) {
			addCommandToProcess(BluetoothPlayer.COMMAND_GET_MEDIA_LIBRARY);
		}
	}

	/*****************************************************************************/

}
