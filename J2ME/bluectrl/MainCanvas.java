package bluectrl;

import java.io.IOException;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.lcdui.game.TiledLayer;
import javax.microedition.m3g.Camera;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Loader;
import javax.microedition.m3g.Object3D;
import javax.microedition.m3g.World;


/**
 * Obsluga ekranu midletu
 * @author Kuba Odias
 * @version 0.2
 */
public class MainCanvas extends GameCanvas implements Runnable
{
	/********************************VARIABLES************************************/
	
	/**Glowny obiekt reprezentujacy scene*/
	private World scene;	
	
	/** Aktuanie wyswietlany ekran */
	private int displayedScreen = PLAYER_SCREEN;
	
	/** Obiekt reprezentujacy tlo aplikacji */
	private TiledLayer backgroundLayer;
	
	/** Sprite przedstawiajacy ikone 'Play / Pause' */
	private Sprite playPauseSprite;
	
	/** Sprite przedstawiajacy ikone 'Next' */
	private Sprite nextSprite;
	
	/** Sprite przedstawiajacy ikone 'Previous' */
	private Sprite previousSprite;
	
	/** Sprite przedstawiajacy male logo Bluetooth */
	private Sprite bluetoothLogoSmallSprite;
	
	/** Zmienna informujaca czy jst przytrzymany przycisk 'Play/Pause' */
	private boolean firePressed = false;
	
	/** Zmienna informujaca czy jst przytrzymany przycisk 'Next' */
	private boolean nextPressed = false;
	
	/** Zmienna informujaca czy jst przytrzymany przycisk 'Previous' */
	private boolean previousPressed = false;
	
	/** Wysokosc domyslnej czcionki */
	private int fontHeight;	
	
	/** Szerokosc i wysokosc ekranu komorki */
	private int screenWidth, screenHeight;
	
	/** Obiekt klasy Player przechowujacej informacje o odtwarzanych plikach, playliste */
	private BluetoothPlayer bluetoothPlayer;

	/** Indeks wybranego elementu z biblioteki muzycznej na wyswietlaczu */
	private int mediaLibrarySelectedItemOnScreen;
	
	/** Liczba elementow biblioteki muzycznej wyswietlonych na wyswietlaczu */
	private int mediaLibraryItemsNumberOnScreen;
	
	/** Obiekt odpowiedzialny za odtwarzanie dzwiekow w aplikacji */
	private SoundPlayer soundPlayer;
	
	/** Zmienna uzywana do narysowania okregu obrazujacego postep wykrywania urzadzen Bluetooth */
	private int inquiryProgress = 0;
	
	/** Zmienna informujaca przez jaki czas ma byc pokazywana glosnosc muzyki */
	private int showVolume = 0;
	
	/** Poziom glosnosci odtwarzacza muzycznego na komputerze */
	private int volumeLevel;
	
	/** Polozenie przyciskow zmieniajace sie podczas wykrycia serwera */
	private int buttonsLocation;
	
	private int textPos0, textPos1;
	
	private int waitTimeText0, waitTimeText1;
	
	private String lastText0, lastText1;
	
	/*****************************************************************************/
	
	
	/********************************CONST****************************************/
	
	/** Wyswietlany jest ekran odtwarzacza */
	public static final int PLAYER_SCREEN = 0;
	
	/** Wyswietlany jest ekran biblioteki muzycznej */
	public static final int MEDIA_LIBRARY_SCREEN = 1;
	
	/** Indeks ikony 'Play' we wczytanym obrazku z ikonami */
	private static final int PLAY_ICON = 0;
	
	/** Indeks ikony 'Pause' we wczytanym obrazku z ikonami */
	private static final int PAUSE_ICON = 1;
	
	/** Indeks ikony 'Next' we wczytanym obrazku z ikonami */
	private static final int NEXT_ICON = 2;
	
	/** Indeks ikony 'Previous' we wczytanym obrazku z ikonami */
	private static final int PREVIOUS_ICON = 3;

	/** Indeks wcisnietej ikony 'Play' we wczytanym obrazku z ikonami */
	private static final int PLAY_ICON_PRESSED = 4;
	
	/** Indeks wcisnietej ikony 'Pause' we wczytanym obrazku z ikonami */
	private static final int PAUSE_ICON_PRESSED = 5;
	
	/** Indeks wcisnietej ikony 'Next' we wczytanym obrazku z ikonami */
	private static final int NEXT_ICON_PRESSED = 6;
	
	/** Indeks wcisnietej ikony 'Previous' we wczytanym obrazku z ikonami */
	private static final int PREVIOUS_ICON_PRESSED = 7;
	
	/** Wybrany zostal nastepny element w bibliotece muzycznej */
	private static final int NEXT_ITEM=0;
	
	/** Wybrany zostal poprzedni element w bibliotece muzycznej */
	private static final int PREVIOUS_ITEM=1;
	
	/** Kolor tla */
	private static final int BACKGROUND_COLOR = 3684605;
	
	/** Czas oczekiwania podczas skonczenia przewijania tekstu */
	private static final int SCROLL_TIME_WAIT = 20;

	/*****************************************************************************/
	
	
	/*******************************METHODS***************************************/
	
	/** Konstruktor obiektu klasy MainCanvas 
	 * @author Kuba Odias
	 * @version 1.0
	 * @throws IOException
	 */
    public MainCanvas() throws IOException 
    {
    	super(true);
    	
    	soundPlayer = new SoundPlayer();	// uruchomienie watku odpowiedzialnego za odtwarzanie dzwiekow
        new Thread(soundPlayer).start();

    	screenWidth = getWidth();
    	screenHeight = getHeight();
    	fontHeight = Font.getDefaultFont().getHeight();
    	
    	mediaLibraryItemsNumberOnScreen = (screenHeight - 16) / (fontHeight + 5);
    	
    	buttonsLocation = 70;
    	
        loadResources();	//utworzenie obiektow warstwy tla i tekstur
        
        bluetoothPlayer = new BluetoothPlayer();
        Thread appThread = new Thread(this);	// uruchomienie watku odpowiedzialnego za wyswietlanie grafiki
        appThread.start();
    }

    /** Metoda ustawiajaca wartosc zmiennej displayedScreen
     * @author Kuba Odias
	 * @version 1.0
     * @param d		Nowa wartosc zmiennej displayedScreen
     */
    public void setDisplayedScreen(int d)
    {
    	displayedScreen = d;
    }
    
    /** Metoda pobierajaca wartosc zmiennej displayedScreen
     * @author 	Kuba Odias
	 * @version 1.0
     * @return	Wartosc zmiennej displayedScreen
     */
    public int getDisplayedScreen()
    {
    	return displayedScreen;
    }
    
    /** Metoda odpowiedzialna za wyswietlenie i animacje trojwymiarowego logo 
     * @author Kuba Odias
	 * @version 1.0
	 */
    public void displayLogo()
    {
    	try
    	{
	    	int keys;
	    	int frames = 0;
	    	float angle = 10.0f;	// kat o jaki obraca sie logo w animacji
	    	
	    	Graphics3D g3d = Graphics3D.getInstance();
			Graphics g = getGraphics();
			
	    	//setFullScreenMode(true);

			Thread.sleep(100);
	    	
	        //zaladowanie obiektu zapisanego w pliku m3g
	        Object3D[] objects = Loader.load("/res/blue_logo.m3g");
			
	        scene = (World)objects[0];
	        
	        Camera camera = new Camera();
	        camera.setPerspective(30.0f, (float) getWidth() / (float) getHeight(), 1.0f, 1000.0f);		
	        camera.setTranslation(0.0f, 0.0f, 24.0f);
	        scene.addChild(camera);
	        scene.setActiveCamera(camera);
	        
	        scene.getChild(0).preRotate(180.0f, 1.0f, 0.0f, 0.0f);
	        
	        keys = getKeyStates();
	
	        while(((keys & FIRE_PRESSED) == 0) && (frames < 36))	// oczekiwanie na nacisniecie klawisza
	        {
	        	g3d.bindTarget(g);
	
	        	scene.getChild(0).preRotate(angle, 0.0f, 0.7f, 1.0f);
		        //wyrenderowanie sceny
		        g3d.render(scene); 
		        //zwolnienie kontekstu graficznego
		        g3d.releaseTarget();
		       	      
		        flushGraphics();	
		        
				Thread.sleep(30);	// uspienie watku na 30ms
	 
	        	keys = getKeyStates();
	        	
	        	frames++;
	        }
	        
	        while(((getKeyStates() & FIRE_PRESSED) != 0) && (frames < 36)) { } // oczekiwanie na zwolnienie przycisku fire
	        	
	        
	        if((keys & FIRE_PRESSED) == 0)	// jesli przycisk nie zostal wcisniety, tylko animacja dobiegla konca
	        	Thread.sleep(1000);
    	}
    	catch (InterruptedException e) 
		{
    		soundPlayer.play(SoundPlayer.ERROR_SOUND);
    		System.out.println("Praca watku przerysowujacego zawartosc ekranu zostala przerwana!");
			e.printStackTrace();
		}
    	catch (IOException e) 
		{
    		soundPlayer.play(SoundPlayer.ERROR_SOUND);
    		System.out.println("Blad podczas wczytywania pliku z zasobow!");
			e.printStackTrace();
		}
    	catch(Exception e)
    	{
    		soundPlayer.play(SoundPlayer.ERROR_SOUND);
    		System.out.println("Nieznany wyjatek");
    		e.printStackTrace();
    	}
    }
    
    
    /** Metoda wczytujaca obrazki z zasobow i tworzaca tlo oraz ikony 
     * @author Kuba Odias
	 * @version 0.7
	 * @throws IOException
	 */
    public void loadResources() throws IOException
    {
    	int i, j;
    	
    	Image image = Image.createImage ("/res/layer.png");	// wczytanie obrazku tla
    	Image iconsImage = Image.createImage ("/res/icons.png");	// wczytanie obrazu przechowujacego ikony przyciskow
    	Image logoImage = Image.createImage("/res/blue_logo_small.png");	// wczytanie obrazku przechowujacego male logo Bluetooth
        int rows = getHeight() / 16;
        int cols = getWidth() / 16;
        
        backgroundLayer = new TiledLayer(cols, rows, image, 16, 16);
        backgroundLayer.setCell(0, 0, 3);
        
        for(i = 0; i < cols - 2; i++)
        	backgroundLayer.setCell(i+1, 0, 2);
        backgroundLayer.setCell(cols - 1, 0, 1);
        for(i = 1; i < rows - 1; i++)
        {
        	backgroundLayer.setCell(0, i, 4);
        	backgroundLayer.setCell(cols - 1, i, 8);
        }
        backgroundLayer.setCell(0, rows - 1, 5);
        for(i = 0; i < cols - 2; i++)
        	backgroundLayer.setCell(i+1, rows - 1, 6);
        backgroundLayer.setCell(cols - 1, rows - 1, 7);
        
        for(i = 1; i < cols - 1; i++)
        	for(j = 1; j < rows - 1; j++)
        		backgroundLayer.setCell(i, j, 9);
        
        backgroundLayer.setPosition((getWidth()%16) / 2, (getHeight()%16) / 2);	//przesuniecie utworzonej warstwy na srodek wyswietlacza
        
        playPauseSprite = new Sprite(iconsImage, 32, 32);
        playPauseSprite.setFrame(PLAY_ICON);
        nextSprite = new Sprite(iconsImage, 32, 32);
        nextSprite.setFrame(NEXT_ICON);	// ustawienie sprite'a tak, aby wyswietlal ikone 'Next'
        previousSprite = new Sprite(iconsImage, 32, 32);
        previousSprite.setFrame(PREVIOUS_ICON);	// ustawienie sprite'a tak, aby wyswietlal ikone 'Previous'
        bluetoothLogoSmallSprite = new Sprite(logoImage, 8, 14);
        
        
    }
    
    /** Metoda odpowiedzialna za czyszczenie pozostalosci po aplikacji 
     * @author Kuba Odias
	 * @version 0.1
	 */
    public void destroy() 
    {
    	bluetoothPlayer.closeConnection();
    	scene = null;
    	
    	//if(getGameObject() != null)
		//	getGameObject().destroy();
    	
    }
    
    /** Metoda uruchamiana przez watek, odpowiedzialna za przerysowywanie ekranu 
     * @author Kuba Odias
	 * @version 1.0
	 */
	public void run() 
	{
		displayLogo();
		
		//setFullScreenMode(false);
		
		while(true)
		{		
			updateState();	// sprawdzenie stanu przyciskow 
			updateScreen();	// wyswietlenie nowej zawartosci ekranu
	        
			/*if(bluetoothPlayer.getBluetoothError())
			{
				Alert alert = new Alert("Bluetooth Error");
				alert.setString("Bluetooth connection broken. Connecting again in 5 seconds");
	    		alert.setType(AlertType.ERROR);
	    		alert.setTimeout(4000);
				display.setCurrent(alert, this);
				bluetoothPlayer.setBluetoothError(false);
			}
			*/
			try
			{
				Thread.sleep(50);
			}
			catch(InterruptedException e) 
			{
				soundPlayer.play(SoundPlayer.ERROR_SOUND);
				System.out.println("Praca watku przerysowujacego zawartosc ekranu zostala przerwana!");
			}
		}
		
	}
	
	/** Metoda sprawdzajaca stan klawiszy i aktualizujaca zmienne odpowiedzialne za stan gry 
	 * @author Kuba Odias
	 * @version 0.6
	 */
	public void updateState()
	{
		int keys = getKeyStates();
		
		if(bluetoothPlayer.getIsConnectedToServer() == true)
		{
			if(displayedScreen == PLAYER_SCREEN)	// jesli wyswietlany jest ekran odtwarzacz
			{
				if(((keys & FIRE_PRESSED) != 0) && (nextPressed == false) && (previousPressed == false))	// jesli przycisk 'Play / Pause' jest wcisniety, a pozostale nie sa wcisniete
				{
					if(playPauseSprite.getFrame() == PLAY_ICON)	
					{
						playPauseSprite.setFrame(PLAY_ICON_PRESSED);
					}
					else if(playPauseSprite.getFrame() == PAUSE_ICON)
					{			
						playPauseSprite.setFrame(PAUSE_ICON_PRESSED);
					}
					
					if(firePressed == false)
						soundPlayer.play(SoundPlayer.CLICK_SOUND);
					
					firePressed = true;
				}
				else if(((keys & FIRE_PRESSED) == 0) && (firePressed == true)) // przycisk zostal zwolniony
				{
					if(playPauseSprite.getFrame() == PLAY_ICON_PRESSED)	// zmiana stanu przycisku
					{
						bluetoothPlayer.play();
						playPauseSprite.setFrame(PAUSE_ICON);
						
					}
					else if(playPauseSprite.getFrame() == PAUSE_ICON_PRESSED)
					{
						bluetoothPlayer.pause();
						playPauseSprite.setFrame(PLAY_ICON);
					}
					
					firePressed = false;			
				}
				if(((keys & RIGHT_PRESSED) != 0) && (firePressed == false) && (previousPressed == false))	// jesli przycisk 'Next' jest wcisniety, a pozostale nie sa wcisniete
				{
					if(nextSprite.getFrame() == NEXT_ICON)	
						nextSprite.setFrame(NEXT_ICON_PRESSED);
					
					if(nextPressed == false)
					{
						soundPlayer.play(SoundPlayer.CLICK_SOUND);
					}
					
					nextPressed = true;
				}
				else if(((keys & RIGHT_PRESSED) == 0) && (nextPressed == true)) // przycisk zostal zwolniony
				{
					if(nextSprite.getFrame() == NEXT_ICON_PRESSED)	
						nextSprite.setFrame(NEXT_ICON);
					
					bluetoothPlayer.addCommandToProcess(BluetoothPlayer.COMMAND_NEXT);
					nextPressed = false;
					
					if(bluetoothPlayer.getTotalTime() != 0)	// jesli utwor jest odtwarzany lub zapauzowany	
						playPauseSprite.setFrame(PAUSE_ICON);
				}
				
				if(((keys & LEFT_PRESSED) != 0) && (nextPressed == false) && (firePressed == false))	// jesli przycisk 'Previous' jest wcisniety, a pozostale nie sa wcisniete
				{
					if(previousSprite.getFrame() == PREVIOUS_ICON)	
						previousSprite.setFrame(PREVIOUS_ICON_PRESSED);
					
					if(previousPressed == false)
					{
						soundPlayer.play(SoundPlayer.CLICK_SOUND);
					}
					
					previousPressed = true;
				}
				else if(((keys & LEFT_PRESSED) == 0) && (previousPressed == true)) // przycisk zostal zwolniony
				{
					if(previousSprite.getFrame() == PREVIOUS_ICON_PRESSED)	
						previousSprite.setFrame(PREVIOUS_ICON);
					
					bluetoothPlayer.addCommandToProcess(BluetoothPlayer.COMMAND_PREV);
					previousPressed = false;
					
					if(bluetoothPlayer.getTotalTime() != 0)	// jesli utwor jest odtwarzany lub zapauzowany	
						playPauseSprite.setFrame(PAUSE_ICON);
				}
				
				if(((keys & UP_PRESSED) != 0) && (previousPressed == false) && (nextPressed == false) && (firePressed == false))	// jesli przycisk 'Up' jest wcisniety, a pozostale nie sa wcisniete
				{				
					if(previousPressed == false)
					{
						soundPlayer.play(SoundPlayer.CLICK_SOUND);
					}
					
					bluetoothPlayer.volumeUp();
				}
				
				if(((keys & DOWN_PRESSED) != 0) && (previousPressed == false) && (nextPressed == false) && (firePressed == false))	// jesli przycisk 'Down' jest wcisniety, a pozostale nie sa wcisniete
				{
					if(previousPressed == false)
					{
						soundPlayer.play(SoundPlayer.CLICK_SOUND);
					}
					
					bluetoothPlayer.volumeDown();
				}
			}
			else if(displayedScreen == MEDIA_LIBRARY_SCREEN)	// jesli wyswietlany jest ekran biblioteki muzycznej
			{
				if(((keys & FIRE_PRESSED) != 0) && (nextPressed == false) && (previousPressed == false))	// jesli przycisk 'Play / Pause' jest wcisniety, a pozostale nie sa wcisniete
				{
					if(firePressed == false)
						soundPlayer.play(SoundPlayer.CLICK_SOUND);
					firePressed = true;
				}
				else if(((keys & FIRE_PRESSED) == 0) && (firePressed == true)) // przycisk zostal zwolniony
				{
					firePressed = false;			
				}
				if(((keys & DOWN_PRESSED) != 0) && (firePressed == false) && (previousPressed == false))	// jesli przycisk 'Next' jest wcisniety, a pozostale nie sa wcisniete
				{
					nextPressed = true;
				}
				else if(((keys & DOWN_PRESSED) == 0) && (nextPressed == true)) // przycisk zostal zwolniony
				{
					if(bluetoothPlayer.getMediaLibrary().selectItemInMediaLibrary(NEXT_ITEM) == true)	// jesli wybrany utwor nie byl ostatni na liscie
					{
						if(mediaLibrarySelectedItemOnScreen < mediaLibraryItemsNumberOnScreen - 1)	
							mediaLibrarySelectedItemOnScreen++;
					}
					nextPressed = false;
				}
				
				if(((keys & UP_PRESSED) != 0) && (nextPressed == false) && (firePressed == false))	// jesli przycisk 'Previous' jest wcisniety, a pozostale nie sa wcisniete
				{
					previousPressed = true;
				}
				else if(((keys & UP_PRESSED) == 0) && (previousPressed == true)) // przycisk zostal zwolniony
				{
					if(bluetoothPlayer.getMediaLibrary().selectItemInMediaLibrary(PREVIOUS_ITEM) == true)	// jesli wybrany utwor nie byl pierwszy na liscie
					{
						if(mediaLibrarySelectedItemOnScreen > 0)	
							mediaLibrarySelectedItemOnScreen--;
					}
					
					previousPressed = false;
				}
			}
		}
		
		if(volumeLevel != bluetoothPlayer.getVolumeLevel())		// jesli poziom glosnosci sie zmienil
		{
			showVolume = 15;
			volumeLevel = bluetoothPlayer.getVolumeLevel();
		}
		
		if(bluetoothPlayer.getIsConnectedToServer() == false)
		{
			if(buttonsLocation < 70)
				buttonsLocation+=5;
		}
		
		if((bluetoothPlayer.getIsConnectedToServer() == true) && (buttonsLocation > 0))
		{
			buttonsLocation-=5;
		}
		
		if(bluetoothPlayer.getStateChanged())
		{
			String state = bluetoothPlayer.getPlayingState();
			
			if(state != null)
			{
					if(state.equals("PLAY") || state.equals("OTHER"))
					{
						playPauseSprite.setFrame(PAUSE_ICON);
					}
					else if(state.equals("PAUSE") || state.equals("STOP"))
						playPauseSprite.setFrame(PLAY_ICON);
			}
			
			soundPlayer.play(SoundPlayer.POPUP_SOUND);
		}
	}
	
	
	/** Metoda odpowiedzialna za przerysowanie ekranu 
	 * @author Kuba Odias
	 * @version 0.4
	 */
	public void updateScreen()
	{
		Graphics g = getGraphics();
		if(displayedScreen == PLAYER_SCREEN)
		{
			if(backgroundLayer != null)
			{
				backgroundLayer.paint(g);
			}
			
			if((bluetoothPlayer.getIsConnectedToServer() == false) && (bluetoothPlayer.getBluetoothError() == false))
					drawText(g, "Connecting...", 0);
			else if(bluetoothPlayer.getBluetoothError() == true)
					drawText(g, "Disconnected", 0);
			else
			{
				if(bluetoothPlayer.getTitle() != null)
				{
					drawText(g, bluetoothPlayer.getTitle(), 0);
				}
				if(bluetoothPlayer.getArtist() != null)
				{
					drawText(g, bluetoothPlayer.getArtist(), 1);
				}
				
				if(showVolume > 0)
				{
					showVolume--;
					
					drawText(g, "      Volume: " + volumeLevel + "%", 2);
				}
			}
				
			if(bluetoothPlayer.isBluetoothOn())
			{
				if(bluetoothPlayer.getInquiryStarted())
				{
					showInquiryProgress(g, inquiryProgress+=20);
					if(inquiryProgress >= 360)
						inquiryProgress = 0;
				}
				else
				{
					inquiryProgress = 0;
					showInquiryProgress(g, 0);
				}
					
			}
			else
			{
				drawText(g, "Please turn your Bluetooth device on", 1);
			}

			
			playPauseSprite.setPosition(screenWidth / 2 - 16, screenHeight - 66 + buttonsLocation);
			previousSprite.setPosition(screenWidth / 2 - 55, screenHeight - 66 + buttonsLocation);
			nextSprite.setPosition(screenWidth / 2 + 23, screenHeight - 66 + buttonsLocation);
			playPauseSprite.paint(g);
			previousSprite.paint(g);
			nextSprite.paint(g);		
			
			if(bluetoothPlayer.getTotalTime() != 0)
				showProgressBar(g, bluetoothPlayer.getCurrentTime(), bluetoothPlayer.getTotalTime());
			else
				showProgressBar(g, 0, 100);
		}
		else if(displayedScreen == MEDIA_LIBRARY_SCREEN)
		{
			if(backgroundLayer != null)
			{
				backgroundLayer.paint(g);
			}
			
			showMediaLibrary(g, bluetoothPlayer, mediaLibrarySelectedItemOnScreen, mediaLibraryItemsNumberOnScreen);
		}
		
		flushGraphics();
	}
	
	/** Metoda wyswietlajaca pasek postepu nad przyciskami 
	 * @author 			Kuba Odias
	 * @version 		0.9
	 * @param g			Referencja do obiektu klasy Graphics, ktory pozwala na narysowanie pasku postepu
	 * @param current	Aktualny czas trwania piosenki, 0 <= progress <= max
	 * @param max		Calkowity czas trwania piosenki
	 */
	public void showProgressBar(Graphics g, int current, int max)
	{
		int color = g.getColor();	// przechowanie uzywanego koloru
		int progressBarWidth = screenWidth - 25;	// szerokosc paska postepu
		int progress = (progressBarWidth * current) / max; 
		
		if(current == -1)	// jesli timer jest wylaczony
			progress = 0;
		
		g.setColor(110, 110, 110);	// narysowanie szarej obwodki
		g.drawRect(10, screenHeight - 29 + buttonsLocation, progressBarWidth + 3, 17);
		
		g.setColor(90, 90, 90);	// narysowanie ciemnej obwodki
		g.drawRect(11, screenHeight - 28 + buttonsLocation, progressBarWidth + 1, 15);
		
		g.setColor(BACKGROUND_COLOR);
		g.fillRect(12, screenHeight - 27 + buttonsLocation, progressBarWidth, 14);
		
		g.setColor(230, 230, 230);
		g.fillRect(12, screenHeight - 27 + buttonsLocation, progress, 14);
		
		g.setColor(70, 70, 70);
		//if(bluetoothPlayer.getCurrentTimeString() != null)
		g.drawString(bluetoothPlayer.getCurrentTimeString(), screenWidth / 2 - 17, screenHeight - 26 + buttonsLocation, Graphics.TOP | Graphics.LEFT);
		
		g.setColor(color);
		
	}
	
	/** Metoda wyswietlajaca okreg obrazujacy postep wykrywania urzadzen 
	 * @author 			Kuba Odias
	 * @version 		1.0
	 * @param g			Referencja do obiektu klasy Graphics, ktory pozwala na narysowanie pasku postepu
	 * @param progress	Liczba z zakresu 0 - 359: zakres luku
	 */
	public void showInquiryProgress(Graphics g, int progress)
	{
		int c = g.getColor();
		
		g.setColor(255, 255, 255);
		
		g.drawArc(screenWidth - 25, screenHeight - 57, 10, 16, 0, progress);
		
		g.setColor(c);
		
		bluetoothLogoSmallSprite.setPosition(screenWidth - 24, screenHeight - 56);
		bluetoothLogoSmallSprite.paint(g);
	}
	
	/** Metoda sluzaca do wypisania tekstu na ekranie w podanej linii
	 * @author 		Kuba Odias
	 * @version 	0.7
	 * @param g		Referencja do obiektu klasy Graphics, ktory pozwala na wyswietlenie tekstu
	 * @param text	Tekst, ktory ma zostac wypisany
	 * @param line	Numer linii, w ktorej ma zostac wypisany tekst
	 * @return		<code>true</code> jesli wypisanie tekstu powiodlo sie, <code>false</code> w przeciwnym razie
	 */
	public boolean drawText(Graphics g, String text, int line)
	{
		int color = g.getColor();	// przechowanie uzywanego koloru
		int textPos = 0;
		
		g.setColor(BACKGROUND_COLOR);	// zamazanie starego tekstu
		g.fillRect(16, 16 + line*(fontHeight + 3), screenWidth - 32, fontHeight);
		
		if(line == 0)
		{
			if(text.equals(lastText0) == false)		// jesli zmienil sie wyswietlany tekst
			{
				lastText0 = text;
				textPos0 = 0;
				waitTimeText0 = SCROLL_TIME_WAIT;
			}
			textPos = textPos0;
		}
		else if(line == 1)
		{
			if(text.equals(lastText1) == false)		// jesli zmienil sie wyswietlany tekst
			{
				lastText1 = text;
				textPos1 = 0;
				waitTimeText1 = SCROLL_TIME_WAIT;
			}
			textPos = textPos1;
		}
		
		if(Font.getDefaultFont().stringWidth(text) <= screenWidth - 32)		// jesli caly tekst miesci sie na ekranie
		{
			g.setColor(255, 255, 255);	// biala czcionka
			g.drawString(text, 16, 16 + line*(fontHeight + 3), Graphics.TOP | Graphics.LEFT);
		}
		else	// jesli tekst nie miesci sie na ekranie
		{
			String leftTextTmp = "";
			int i = 0;
			
			while(Font.getDefaultFont().stringWidth(leftTextTmp) < textPos)		// ucinanie tekstu dopoki nie miesci sie z lewej strony na ekranie
			{
				leftTextTmp = text.substring(0, ++i); 
			}
			
			String textTmp = text.substring(i, text.length());
			
			boolean textRightCut = false;
			while(Font.getDefaultFont().stringWidth(textTmp) > screenWidth - 32)	// ucinanie tekstu dopoki nie miesci sie z prawej strony na ekranie
			{
				textTmp = textTmp.substring(0, textTmp.length() - 1);
				textRightCut = true;
			}
			
			g.setColor(255, 255, 255);	// biala czcionka
			g.drawString(textTmp, 16/* - Font.getDefaultFont().charWidth(textTmp.charAt(0))*/, 16 + line*(fontHeight + 3), Graphics.TOP | Graphics.LEFT);
			
			g.setColor(BACKGROUND_COLOR);	// zamazanie tekstu po bokach
			g.fillRect(screenWidth - 16, 16 + line*(fontHeight + 3), 11, fontHeight);
			//g.fillRect(5, 16 + line*(fontHeight + 3), 11, fontHeight);
			
			if(line == 0)
			{
				if((textRightCut == false) && (waitTimeText0 == 0))	// jesli tekst przewinal sie do konca
					waitTimeText0 = SCROLL_TIME_WAIT;
				
				if(waitTimeText0 == 0)
					textPos0 += 2;		// przewiniecie tekstu
				else		// jesli tekst ma byc wyswietlany w miejscu bez przesuwania
				{
					waitTimeText0--;
					if((waitTimeText0 == 0) && (textPos0 != 0))		// w przypadku skonczenia czasu oczekiwania ma sie rozpoczac przewijanie od nowa
					{
						waitTimeText0 = SCROLL_TIME_WAIT;
						textPos0 = 0;
					}
				}
			}
			else if(line == 1)
			{
				if((textRightCut == false) && (waitTimeText1 == 0))	// jesli tekst przewinal sie do konca
					waitTimeText1 = SCROLL_TIME_WAIT;
				
				if(waitTimeText1 == 0)
					textPos1 += 2;		// przewiniecie tekstu
				else		// jesli tekst ma byc wyswietlany w miejscu bez przesuwania
				{
					waitTimeText1--;
					if((waitTimeText1 == 0) && (textPos1 != 0))		// w przypadku skonczenia czasu oczekiwania ma sie rozpoczac przewijanie od nowa
					{
						waitTimeText1 = SCROLL_TIME_WAIT;
						textPos1 = 0;
					}
				}
			}
		}	
		
		g.setColor(color);
		return true;
	}	
	
	/** Metoda sluzaca do ponownego polaczenia sie z serwerem
	 * @author 			Kuba Odias
	 * @version 		0.5
	 */
	public void reconnectToServer()
	{
		bluetoothPlayer.closeConnection();		// wystarczy sie rozlaczyc, reszta zostanie wykonana w metodzie run klasy BluetoothPlayer
	}
	
	/** Metoda wyswietlajaca biblioteke muzyczna na wyswietlaczu
	 * @author 							Kuba Odias
	 * @version 						0.2
	 * @param g							Referencja do obiektu klasy Graphics, ktory pozwala na wyswietlenie tekstu
	 * @param player					Referencja do obiektu klasy odtwarzacza muzycznego
	 * @param screenSelectedItemIndex	Indeks wybranego elementu na wyswietlaczu
	 * @param screenNumberOfItems		Liczba elementow wyswietlonych na wyswietlaczu
	 */
	public void showMediaLibrary(Graphics g, BluetoothPlayer player, int screenSelectedItemIndex, int screenNumberOfItems)
	{
		int color = g.getColor();	// przechowanie uzywanego koloru
		
		g.setColor(255, 255, 255);	// biala czcionka
		for(int i=0; i < screenNumberOfItems; i++)
			g.drawString("piosenka " + i, 10, i*(fontHeight + 5) + 8, Graphics.TOP | Graphics.LEFT);
		
		g.drawRect(7, screenSelectedItemIndex*(fontHeight + 5) + 6, screenWidth - 16, fontHeight + 1);
		
		g.setColor(color);
	}
	
	

	/*****************************************************************************/
}