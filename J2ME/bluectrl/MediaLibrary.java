package bluectrl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;

import org.kxml.parser.*;
import org.kxml.kdom.*;
import org.kxml.*;


/**
 * Klasa odpowiedzialna za zaladowanie biblioteki muzycznej
 * @author Kuba Odias
 * @version 0.2
 */
public class MediaLibrary 
{

	/**
	 * Wewnetrzna klasa przechowujaca informacje o utworze wczytanym z playlisty
	 * @author Kuba Odias
	 * @version 0.5
	 */
	private class Track
	{
		/** Dlugosc utworu w sekundach */
		int length;
		
		/** Tytul utworu */
		String title;
		
		/** Nazwa artysty */
		String artist;
		
		/** Nazwa albumu */
		String album;
		
		
		/** Konstruktor obiektu klasy Track
		 * @author Kuba Odias
		 * @version 1.0
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
	private XmlParser xmlParser;
	
	/** Indeks wybranego utworu w bibliotece muzycznej */
	private int mediaLibrarySelectedItem;	/////-1
	
	/** Tablica przechowujaca informacje o utworach */
	private Hashtable mediaLibraryItems;
	
	/** Rozmiar biblioteki muzycznej pobieranej z komputera w bajtach */
	private int mediaLibrarySize;

	/*****************************************************************************/
	
	
	/*******************************CONSTANTS*************************************/
	
	/** Wybrany zostal nastepny element w bibliotece muzycznej */
	private static final int NEXT_ITEM=0;
	
	/** Wybrany zostal poprzedni element w bibliotece muzycznej */
	private static final int PREVIOUS_ITEM=1;
	
	public static final String playlistFilename = "playlist.xml";

	/*****************************************************************************/

	
	/*******************************METHODS***************************************/
	
	/** Konstruktor obiektu klasy MediaLibrary
	 * @author Kuba Odias
	 * @version 1.0
	 */
	public MediaLibrary() {
		xmlParser = null;
		mediaLibraryItems = new Hashtable();
		mediaLibrarySelectedItem = 0;
	}
	
	/** Akcesor zmiennej mediaLibrarySelectedItem
	 * @author Kuba Odias
	 * @version 1.0
	 * @return Wartosc zmiennej mediaLibrarySelectedItem
	 */
	public int getMediaLibrarySelectedItem() {
		return mediaLibrarySelectedItem;
	}
	
	
	/** Metoda zwraca ilosc bajtow, ktora zostala przetworzona przez parser XML, w przyblizeniu ilosc bajtow pobrana przez Bluetooth
	 * @author		Kuba Odias
	 * @version	1.0
	 * @return		Ilosc bajtow przetworzona przez parser XML
	 */
	public int getMediaLibraryDownloadedBytes() {
		if (xmlParser != null)
			return xmlParser.getBufPos();
		
		return 0;
	}
	
	/** Akcesor zmiennej mediaLibrarySize
	 * @author		Kuba Odias
	 * @version	1.0
	 * @return		Wartosc zmiennej mediaLibrarySizes
	 */
	public int getMediaLibrarySize() {
		return mediaLibrarySize;
	}
	
	/** Metoda ustawia wartosc zmiennej mediaLibrarySize
	 * @author		Kuba Odias
	 * @version	1.0
	 * @param size	 Nowa wartosc zmiennej mediaLibrarySize
	 */
	public void setMediaLibrarySize(int size) {
		mediaLibrarySize = size;
	}
	
	/** Metoda parsePlaylist wczytuje plik XML zawierajacy liste utworow a nastepnie parsuje ten plik
	 * @author Kuba Odias
	 * @version 0.6
	 */
	public void parsePlaylist(InputStream in) {
		Document doc = new Document();
		int id, length;
		String title, artist, album;

		try {
			InputStreamReader isr = new InputStreamReader(in);
			
			xmlParser = new XmlParser( isr );
			
			// Pass the parser to the document. At this point the
			// entire resource is parsed and now resides in memory.
			doc.parse( xmlParser );

			xmlParser = null;
		} 
		catch (IOException ioe) {
			ioe.printStackTrace();

			xmlParser = null;
			doc = null;
			
			return;
		} 

		// Now we get the root element which is "playlist"
		Element root = doc.getRootElement();

		int child_count = root.getChildCount();

		for (int i = 0; i < child_count ; i++ ) {
			if (root.getType(i) != Xml.ELEMENT) {
				continue;
			}
						
			Element kid = root.getElement(i);
			
			// 'item' is an only valid element
			if (!kid.getName().equals("item")) {

				continue;
			}
			
			// clear info about the track
			id = 0;
			length = 0;
			title = artist = album = null;
			
			// new item was found - add it's child to the structure
			int address_item_count = kid.getChildCount();

			for (int j = 0; j < address_item_count ; j++) {
				if (kid.getType(j) != Xml.ELEMENT) {
					continue;
				}
				
				Element item = kid.getElement(j);

				if (item.getName().equals("id")) {
					id = Integer.parseInt(item.getText(0));
				}
				else if (item.getName().equals("Length")) {
					length = Integer.parseInt(item.getText(0));
				}
				else if (item.getName().equals("Title")) {
					title = item.getText(0);
				}
				else if (item.getName().equals("Artist")) {
					artist = item.getText(0);
				}
				else if (item.getName().equals("Album")) {
					album = item.getText(0);
				}
				
				item = null;
			}
			
			Track track = new MediaLibrary.Track(length, title, artist, album);
			Integer id_int = new Integer(id);
			
			// add new music track to the hashtable
			mediaLibraryItems.put(id_int, track);
			
			kid = null;
		}
	}	
	
	/** Metoda wybierajaca nowy element w bibliotece muzycznej
	 * @author 		Kuba Odias
	 * @version 		1.0
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
	 * @author 		Kuba Odias
	 * @version 		1.0
	 * @param index	Indeks szukanego utworu na liscie
	 * @return 		Nazwa utworu i wykonawcy
	 */
	public String getItem(int index) {
		if (index > mediaLibraryItems.size())
			return null;
		
		Track track = (Track)mediaLibraryItems.get(new Integer(index));
		
		return (track.artist + "-" + track.title);
	}
}
