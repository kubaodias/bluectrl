package bluectrl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.Graphics;

import nanoxml.kXMLElement;
import nanoxml.kXMLParseException;

/**
 * Klasa odpowiedzialna za zaladowanie biblioteki muzycznej
 * @author Kuba Odias
 * @version 0.8
 */
public class MediaLibrary {

	/**
	 * Wewnetrzna klasa przechowujaca informacje o utworze wczytanym z playlisty
	 */
	public class Track {
		/** Dlugosc utworu w sekundach */
		int length;
		
		/** Tytul utworu */
		String title;
		
		/** Nazwa artysty */
		String artist;
		
		/** Nazwa albumu */
		String album;
		
		
		/** Konstruktor obiektu klasy Track
		 * @param l	Czas trwania utworu w sekundach
		 * @param t	Tytul utworu
		 * @param art	Nazwa artysty
		 * @param alb	Nazwa albumu
		 */
		Track(int l, String t, String art, String alb) {
			length = l;
			title = t;
			artist = art;
			album = alb;
		}
	};
	
	/*******************************VARIABLES*************************************/
	
	/** Obiekt odpowiedzialny za parsowanie pliku XML otrzymanego przez Bluetooth */
	//private XmlParser xmlParser;
	
	/** Indeks wybranego utworu w bibliotece muzycznej */
	private int mediaLibrarySelectedItem;	/////-1
	
	/** Tablica przechowujaca informacje o utworach */
	public Hashtable mediaLibraryItems;
	
	/** Rozmiar biblioteki muzycznej pobieranej z komputera w bajtach */
	private int mediaLibrarySize;
	
	/** Number of bytes already downloaded to the mobile device */
	private int mediaLibraryDownloadedBytes;

	/** Boolean variable that tells if media library was downloaded from server and parsed */
	private boolean isLibraryDownloadedAndParsed;
	
	/*****************************************************************************/
	
	
	/*******************************CONSTANTS*************************************/
	
	/** Wybrany zostal nastepny element w bibliotece muzycznej */
	private static final int NEXT_ITEM=0;
	
	/** Wybrany zostal poprzedni element w bibliotece muzycznej */
	private static final int PREVIOUS_ITEM=1;
	
	/** Maximal size of one packet of data with playlist that is send over Bluetooth */
	public static final int DOWNLOAD_PACKET_SIZE = 32;

	/*****************************************************************************/

	
	/*******************************METHODS***************************************/
	
	/** Konstruktor obiektu klasy MediaLibrary
	 */
	public MediaLibrary() {
		//xmlParser = null;
		mediaLibraryItems = new Hashtable();
		mediaLibrarySelectedItem = 0;
		isLibraryDownloadedAndParsed = false;
	}
	
	/** Akcesor zmiennej mediaLibrarySelectedItem
	 * @return Wartosc zmiennej mediaLibrarySelectedItem
	 */
	public int getMediaLibrarySelectedItem() {
		return mediaLibrarySelectedItem;
	}
	
	
	/** Metoda zwraca ilosc bajtow, ktora zostala przetworzona przez parser XML, w przyblizeniu ilosc bajtow pobrana przez Bluetooth
	 * @return		Ilosc bajtow przetworzona przez parser XML
	 */
	public int getMediaLibraryDownloadedBytes() {
		//if (xmlParser != null)
			//return xmlParser.getBufPos();
		
		return mediaLibraryDownloadedBytes;
	}
	
	/** Akcesor zmiennej mediaLibrarySize
	 * @return		Wartosc zmiennej mediaLibrarySizes
	 */
	public int getMediaLibrarySize() {
		return mediaLibrarySize;
	}
	
	/** Akcesor zmiennej libraryDownloadedAndParsed
	 * @return		Wartosc zmiennej libraryDownloadedAndParsed
	 */
	public boolean getLibraryDownloadedAndParsed() {
		return isLibraryDownloadedAndParsed;
	}
	
	/** Metoda ustawia wartosc zmiennej mediaLibraryDownloadedBytes
	 * @param size	 Nowa wartosc zmiennej mediaLibraryDownloadedBytes
	 */
	public void setMediaLibraryDownloadedBytes(int size) {
		mediaLibraryDownloadedBytes = size;
	}
	
	/** Metoda ustawia wartosc zmiennej mediaLibrarySize
	 * @param size	 Nowa wartosc zmiennej mediaLibrarySize
	 */
	public void setMediaLibrarySize(int size) {
		mediaLibrarySize = size;
	}
	
	/** parsePlaylist method reads an XML file containing info about music tracks, then parses this file 
	 * @param in 	Input stream used to read music playlist
	 */
	public void parsePlaylist(InputStream in) {
		byte[] byteArray;
		int id, length;
		String title, artist, album;

		mediaLibraryDownloadedBytes = 0;
		isLibraryDownloadedAndParsed = false;
		try {
	        byteArray = new byte[this.mediaLibrarySize + 1];
	        int bytesRead;
	        int bytesToRead;

	        while (mediaLibraryDownloadedBytes < mediaLibrarySize) {
	        	// if there's less than DOWNLOAD_PACKET_SIZE to read from Bluetooth socket	        	
	        	if (((mediaLibrarySize - mediaLibraryDownloadedBytes) / DOWNLOAD_PACKET_SIZE) == 0)
	        		bytesToRead = mediaLibrarySize - mediaLibraryDownloadedBytes;
	        	else
	        		bytesToRead = DOWNLOAD_PACKET_SIZE; 
	        	
	        	if((bytesRead=(byte)in.read(byteArray, mediaLibraryDownloadedBytes, bytesToRead)) >= 0) {
	        		mediaLibraryDownloadedBytes += bytesRead;
	        	}
	        	else
	        		break;
	        }  
			
			kXMLElement root = new kXMLElement();
		    root.parseByteArray(byteArray, 0, mediaLibrarySize);
		    
		    Vector list = root.getChildren();
            
            for( int i = 0; i < list.size(); ++i ){
                kXMLElement node = (kXMLElement) list.elementAt( i );
                String      tag = node.getTagName();
                
                if (tag == null) continue;
                if (!tag.equals("item")) continue;
                if (tag.equals("item")) {
                	if (node.countChildren() != 0) {
                		Vector itemList = node.getChildren();
      
                		id = 0;
            			length = 0;
            			title = artist = album = null;
            			
                		for( int j = 0; j < itemList.size(); j++){
                            kXMLElement itemNode = (kXMLElement)itemList.elementAt( j );
                            String      itemTag = itemNode.getTagName();
                            
                            if (itemTag == null) continue;
                            else if (itemTag.equals("id")) {
            					id = Integer.parseInt(itemNode.getContents());
            				}
            				else if (itemTag.equals("Length")) {
            					length = Integer.parseInt(itemNode.getContents());
            				}
            				else if (itemTag.equals("Title")) {
            					title = itemNode.getContents();
            				}
            				else if (itemTag.equals("Artist")) {
            					artist = itemNode.getContents();
            				}
            				else if (itemTag.equals("Album")) {
            					album = itemNode.getContents();
            				}
                          
                		}
                		
                		Track track = new MediaLibrary.Track(length, title, artist, album);
    					Integer id_int = new Integer(id);
    					
    					// add new music track to the hashtable
    					mediaLibraryItems.put(id_int, track);
                	}
                }
   			}
			
			// mark library as read
            isLibraryDownloadedAndParsed = true;
		}
		catch( kXMLParseException pe ) {
			pe.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		} 
	
	}	
	
	/** Metoda wybierajaca nowy element w bibliotece muzycznej
	 * @param item		NEXT_ITEM, lub PREVIOUS_ITEM
	 * @return 		<code>true</code> jesli zmiana elementu powiodla sie, <code>false</code> w przeciwnym razie
	 */
	public boolean selectItemInMediaLibrary(int item) {
		if(item == PREVIOUS_ITEM) {
			if(mediaLibrarySelectedItem == 0)	// wybrany jest juz pierwszy element na liscie
				return false;
			mediaLibrarySelectedItem--;
			return true;
		}
		else if(item == NEXT_ITEM)	{
			if(mediaLibrarySelectedItem == mediaLibraryItems.size() - 1)	// wybrany jest juz ostatni element na liscie
				return false;
			mediaLibrarySelectedItem++;
			return true;
		}
		
		return false;	// aplikacja nie powinna sie tu znalezc
	}
	
	/** Metoda zwraca nazwe i wykonawce szukanego utworu
	 * @param index	Indeks szukanego utworu na liscie
	 * @return 		Nazwa utworu i wykonawcy
	 */
	public String getItem(int index, Graphics g) {
		if (index >= mediaLibraryItems.size())
			return null;
				
		Track track = (Track)mediaLibraryItems.get(new Integer(index));
		
		return (track.artist + "-" + track.title);
	}
}
