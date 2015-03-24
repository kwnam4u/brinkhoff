package showmap;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import drawables.*;
import util.*;

/**
 * Abstract applet for showing an interactive map.
 * The applet allows navigating, zooming and retrieval of object information.
 *
 * @version 2.42	02.07.01	computeURL modified
 * @version	2.41	01.09.00	unicode selected at start
 * @version	2.40	28.06.00	support of DO210-files and presentation modes, CPUTimer instead of Timer
 * @version	2.30	01.03.00	x/yIntoCoord added, index added to readDrawables
 * @version	2.20	07.12.99	support of two languages
 * @version	2.10	19.08.99	support of DrawableObjects
 * @version	2.00	19.03.99	adapted to Drawables v2.0, changed to an abstract class
 * @author Thomas Brinkhoff
 */

public abstract class ShowMap extends Applet implements ActionListener, ItemListener, MouseListener, MouseMotionListener {

	/**
	 * Status "startend".
	 */
	protected static final int STARTING = 0;
	/**
	 * Status "gestarted".
	 */
	protected static final int STARTED = 2;
	/**
	 * Status "aktiv: gestarted und bereit f�r Interaktion".
	 */
	protected static final int ACTIVE = 3;
	/**
	 * Status "vollst�ndig geladen".
	 */
	protected static final int COMPLETE = 4;

	/**
	 * Sprache Englisch.
	 */
	protected static final int ENGLISH = 0;
	/**
	 * Sprache Deutsch.
	 */
	protected static final int GERMAN = 1;

	/**
	 * ID der Update-Timers.
	 */
	protected CPUTimer updateTimer = new CPUTimer();
	/**
	 * Wartezeit f�r n�chsten Update in Millisekunden.
	 */
	protected static final long UPDATETIME = 3000;

	/**
	 * Anzahl der Layer.
	 */
 	protected int numOfLayers = 5;
	/**
	 * Container �ber Drawable-Objekte.
	 */
 	protected DrawableObjects drawableObjects = null;

 	/**
	 * Faktor des Basis-Ma�stabs (gibt an, mit welchen Faktor der Ma�stab zu multiplizieren ist, um die eigentliche Ma�stabszahl zu erhalten).
	 */
 	protected int baseScaleFactor = 1000000;
	/**
	 * Breite der Gesamtkarte im Basis-Ma�stab 1 in Pixel.
	 */
	protected int mapWidth = 300;
	/**
	 * H�he der Gesamtkarte im Basis-Ma�stab 1 in Pixel.
	 */
	protected int mapHeight = 300;
	/**
	 * x-Koordinate des Mittelpunkts des akt. angezeigten Kartenausschnitts im akt. Ma�stab.
	 */
	protected int viewMapX = 150;
	/**
	 * y-Koordinate des Mittelpunkts des akt. angezeigten Kartenausschnitts im akt. Ma�stab.
	 */
	protected int viewMapY = 150;
	/**
	 * detailiertester Ma�stab (in Ma�stabseinheiten).
	 */
	protected int maxScale = 1;
	/**
	 * gr�bster Ma�stab (in Ma�stabseinheiten).
	 */
	protected int minScale = 320;
	/**
	 * aktueller Ma�stab (in Ma�stabseinheiten).
	 */
	protected int scale = 160;
	/**
	 * Breite der Kartenanzeige (in Pixel).
	 */
	protected int viewWidth = 300;
	/**
	 * H�he der Kartenanzeige (in Pixel).
	 */
	protected int viewHeight = 300;
	/**
	 * Linke Position der Kartenanzeige (in Pixel).
	 */
	protected int viewX = 10;
	/**
	 * Obere Position der Kartenanzeige (in Pixel).
	 */
	protected int viewY = 10;
	/**
	 * Obere Position des Ein-/Ausgabebereichs (in Pixel).
	 * Belegung mit negativen Wert bewirkt,
	 * da� der Ausgabebereich oberhalb der Karte dargestellt wird.
	 * Ansonsten wird er unterhalb der Karte dargestellt.
	 */
	protected int panelY = 1;
	/**
	 * H�he des Ein-/Ausgabebereichs (in Pixel).
	 */
	protected int panelHeight = 163;
	/**
	 * Hintergrundfarbe.
	 */
	protected Color backgroundColor = ColorDefiner.getDefaultColor();
	/**
	 * Basis-Kartenfarbe.
	 */
	protected Color mapColor = new Color(120,255,255);
	/**
	 * Wird der Wechsel auf Unicode-Darstellung unterst�tzt?
	 */
	protected boolean unicodeSupported = false;
	/**
	 * gew�hlte Sprache
	 */
	protected int language = ENGLISH;

	/**
	 * Applet-Status
	 */
	protected int state = STARTING;
	/**
	 * Image f�r Double-Buffering.
	 */
	private Image doubleBuffer = null;
	/**
	 * letzte x-Position der Maus beim MousePressed-Event.
	 */
	private int lastMouseXPos = 0;
	/**
	 * letzte y-Position der Maus beim MousePressed-Event.
	 */
	private int lastMouseYPos = 0;
	/**
	 * letzte x-Position der Maus beim MouseDrag-Event.
	 */
	private int lastDragXPos = 0;
	/**
	 * letzte y-Position der Maus beim MouseDrag-Event.
	 */
	private int lastDragYPos = 0;
	/**
	 * Wird aktuell eine Info-Box angezeigt?
	 */
	protected boolean infoIsShown = false;

	/**
	 * Labels
	 */
	private Label ivjClickInfoLabel = null;
	private Label ivjCopyrightLabel = null;
	private Label ivjNameLabel = null;
	private Label ivjPressInfoLabel = null;
	private Label ivjScaleLabel = null;
	private Label ivjShiftClickInfoLabel = null;
	private Label ivjTagLabel = null;
	private Label ivjValueLabel = null;
	/**
	 * Buttons
	 */
	private Button ivjEastButton = null;
	private Button ivjNorthButton = null;
	private Button ivjSouthButton = null;
	private Button ivjWestButton = null;
	private Button ivjZoomInButton = null;
	private Button ivjZoomOutButton = null;
	/**
	 * Checkbox
	 */
	private Checkbox ivjUnicodeCheckbox = null;

/**
 * Event handling for the ActionListener interface.
 * @param e the event
 */
public void actionPerformed (java.awt.event.ActionEvent e) {
	if (e.getSource() == getZoomInButton())
		zoomIn();
	if (e.getSource() == getZoomOutButton())
		zoomOut();
	if (e.getSource() == getNorthButton())
		moveNorth();
	if (e.getSource() == getWestButton())
		moveWest();
	if (e.getSource() == getEastButton())
		moveEast();
	if (e.getSource() == getSouthButton())
		moveSouth();
}
/**
 * Adds the components to the applet.
 */
protected void addComponentsToApplet () {
	// Buttons
	add(getEastButton(), getEastButton().getName());
	add(getWestButton(), getWestButton().getName());
	add(getNorthButton(), getNorthButton().getName());
	add(getSouthButton(), getSouthButton().getName());
	add(getZoomInButton(), getZoomInButton().getName());
	add(getZoomOutButton(), getZoomOutButton().getName());
	// Labels
	add(getScaleLabel(), getScaleLabel().getName());
	add(getNameLabel(), getNameLabel().getName());
	add(getValueLabel(), getValueLabel().getName());
	add(getTagLabel(), getTagLabel().getName());
	add(getClickInfoLabel(), getClickInfoLabel().getName());
	add(getShiftClickInfoLabel(), getShiftClickInfoLabel().getName());
	add(getPressInfoLabel(), getPressInfoLabel().getName());
	add(getCopyrightLabel(), getCopyrightLabel().getName());
	// Checkboxes
	if (unicodeSupported)
		add(getUnicodeCheckbox(), getUnicodeCheckbox().getName());
}
/**
 * Announces the components to the event listeners.
 */
protected void addComponentsToListeners () {
	this.addMouseListener(this);
	this.addMouseMotionListener(this);
	// Buttons
	getZoomInButton().addActionListener(this);
	getZoomOutButton().addActionListener(this);
	getNorthButton().addActionListener(this);
	getWestButton().addActionListener(this);
	getEastButton().addActionListener(this);
	getSouthButton().addActionListener(this);
	// Checkboxes
	if (unicodeSupported)
		getUnicodeCheckbox().addItemListener(this);
}
/**
 * Sets or changes the positions of the components.
 */
public void changeComponentPositions () {
	getEastButton().setBounds (viewX+viewWidth/2+24, viewY+viewHeight+48, 31,29);
	getNorthButton().setBounds(viewX+viewWidth/2-16, viewY+viewHeight+20, 31,29);
	getSouthButton().setBounds(viewX+viewWidth/2-16, viewY+viewHeight+78, 31,29);
	getWestButton().setBounds (viewX+viewWidth/2-56, viewY+viewHeight+48, 31,29);
	getZoomInButton().setBounds (viewX+viewWidth/2-151, viewY+viewHeight+48, 76,29);
	getZoomOutButton().setBounds(viewX+viewWidth/2+76,  viewY+viewHeight+48, 76,29);
	getClickInfoLabel().setBounds     (viewWidth/2+viewX+24, viewHeight+viewY+5,  160,13);
	getShiftClickInfoLabel().setBounds(viewWidth/2+viewX+24, viewHeight+viewY+18, 160,13);
	getPressInfoLabel().setBounds     (viewWidth/2+viewX+24, viewHeight+viewY+31, 160,13);
	getCopyrightLabel().setBounds(viewX, viewY+viewHeight+140, viewWidth-100, 19);
	getScaleLabel().setBounds(viewX, viewY+viewHeight+4, 100,23);
	getNameLabel().setBounds (viewX, viewHeight+viewY+79, viewWidth/2-25,23);
	getValueLabel().setBounds(viewX+viewWidth/2+24, viewHeight+viewY+79, viewWidth/2-25, 23);
	getTagLabel().setBounds  (viewX, viewHeight+viewY+108, viewWidth,23);
	if (unicodeSupported) {
		getTagLabel().setBounds(viewX, viewHeight+viewY+108, viewWidth-70,23);
		getUnicodeCheckbox().setBounds(viewX+viewWidth-70, viewY+viewHeight+104, 70, 23);
	}
}
/**
 * Pr�ft, ob der Anzeige-Mittelpunkt (viewMapX,viewMapY) im zul�ssigen
 * Bereich liegt. Falls nicht, wird er korrigiert. Au�erdem werden die
 * Navigations-/Zoom-Buttons deselektiert, falls keine Navigation/kein Zooming
 * mehr in betroffene Richtung / Ma�stab m�glich ist bzw.
 * wenn der Applet-Status noch nicht "aktiv" ist.
 */
protected void checkViewPoint () {
	boolean set = (state >= ACTIVE);
	if (viewMapX >= mapWidth/scale-viewWidth/2) {
		viewMapX = mapWidth/scale-viewWidth/2;
		getEastButton().setEnabled(false);
	}
	else
		getEastButton().setEnabled(set);
	if (viewMapX <= viewWidth/2) {
		viewMapX = viewWidth/2;
		getWestButton().setEnabled(false);
		getEastButton().setEnabled((viewMapX < mapWidth/scale-viewWidth/2) && set);
	}
	else
		getWestButton().setEnabled(set);
	if (viewMapY >= mapHeight/scale-viewHeight/2) {
		viewMapY = mapHeight/scale-viewHeight/2;
		getSouthButton().setEnabled(false);
	}
	else
		getSouthButton().setEnabled(set);
	if (viewMapY <= viewHeight/2) {
		viewMapY = viewHeight/2;
		getNorthButton().setEnabled(false);
		getSouthButton().setEnabled((viewMapY < mapHeight/scale-viewHeight/2) && set);
	}
	else
		getNorthButton().setEnabled(set);
	getZoomInButton().setEnabled ((scale != maxScale) && set);
	getZoomOutButton().setEnabled ((scale != minScale) && set);
}
/**
 * Computes the URL from the specified name. Relative name are expanded by the code base.
 * If an error occurs, null will be returned.
 * @return the URL
 * @param name absolute orL
 */
protected URL computeURL (String name) {
	try {
		if (name == null)
			return null;
		if ((name.startsWith("http:")) || (name.startsWith("file:")))
		{
			return new URL( name );
		}
		if (name.indexOf(':') > 0)
			return new URL("file:/"+name.replace('\\','/'));
		String docBase = this.getCodeBase().toString();
		return new URL(docBase+name.replace('\\','/'));
	}
	catch (MalformedURLException me) {
		System.err.println("MalformedURLException: " + me);
		return null;
	}
}
/**
 * Deselektiert das bisher darstellte Objekt und selektiert das �bergebene Objekt.
 * Falls null �bergeben wird, wird die Anzeige zur�ckgesetzt.
 * Die abgeleiteten Klassen m�ssen daf�r sorgen,
 * da� die Anzeige der Attribute des �bergebenen Objekts im Applet angezeigt.
 * @param obj Objekt
 */
protected void depictObjectAttributes (DrawableObject obj) {
	// Bisheriges Objekt deselektieren
	//DrawableObject oldSelectedObject = drawableObjects.getSelectedObject();
	drawableObjects.deselect();
	// Falls obj == null, Anzeige l�schen & fertig
	if (obj == null) {
		getNameLabel().setText("...");
		getTagLabel().setText("...");
		getValueLabel().setText("...");
		repaint();
		return;
	}
	// Neues Objekt hervorheben und zeichnen
	drawableObjects.select (obj);
	repaint();
}
/**
 * Draws the map.
 * @param g graphical context
 * @param r clipping rectangle in world coordinates
 * @param scale current scale
 */
protected void drawMap (Graphics g, Rectangle r, int scale) {
	drawableObjects.drawAllObjects (g,r,scale);
	g.setColor (Color.black);
	g.drawRect (0,0,mapWidth/scale+1,mapHeight/scale+1);
}
/**
 * Sucht, ob sich ein sichtbares Objekt an der angegebenen Position befindet.
 * @param mx x-Position in Applet-Pixel-Koordinaten
 * @param my y-Position in Applet-Pixel-Koordinaten
 * @param selectable nur selektierbare Objekte suchen?
 */
protected DrawableObject findObject (int mx, int my, boolean selectable) {
	// Fall: Maus au�erhalb der Karte -> Resultat: null
	if ((mx < viewX) || (mx > viewX+viewWidth) || (my < viewY) || (my > viewY+viewHeight))
		return null;
	// Symbol suchen
	DrawableObject obj = null;
	int px = xIntoCoord(mx);
	int py = yIntoCoord(my);
	obj = drawableObjects.getNextVisibleIntersectingObject (px, py, scale, selectable);
	return obj;
}
/**
 * Information about the applet.
 * @return the information.
 */
public String getAppletInfo() {
	return "Applet ShowMap, Version 2.40\n" + getCopyrightLabel().getText();
}
/**
 * Anlegen / Zur�ckgeben des ClickInfo-Labels.
 * @return ClickInfo-Label
 */
protected Label getClickInfoLabel() {
	if (ivjClickInfoLabel == null) {
		ivjClickInfoLabel = new Label();
		ivjClickInfoLabel.setName("ClickInfo");
		ivjClickInfoLabel.setFont(new Font("dialog", 0, 10));
		if (language == GERMAN)
			ivjClickInfoLabel.setText("Klicken: Objekt-Info");
		else
			ivjClickInfoLabel.setText("click: show info");
	};
	return ivjClickInfoLabel;
}
/**
 * Anlegen / Zur�ckgeben des Copyright-Labels.
 * @return Copyright-Label
 */
protected Label getCopyrightLabel() {
	if (ivjCopyrightLabel == null) {
		ivjCopyrightLabel = new Label();
		ivjCopyrightLabel.setName("CopyrightLabel");
		ivjCopyrightLabel.setFont(new Font("dialog", 0, 10));
		ivjCopyrightLabel.setText("(c) Th. Brinkhoff, 1999-2001, tbrinkhoff@acm.org");
	};
	return ivjCopyrightLabel;
}
/**
 * Anlegen / Zur�ckgeben des East-Buttons.
 * @return East-Button
 */
protected Button getEastButton() {
	if (ivjEastButton == null) {
		ivjEastButton = new Button();
		ivjEastButton.setName("EastButton");
		ivjEastButton.setFont(new Font("dialog", 0, 12));
		if (language == GERMAN)
			ivjEastButton.setLabel("O");
		else
			ivjEastButton.setLabel("E");
	};
	return ivjEastButton;
}
/**
 * Gibt die ID des selektierten Objekts zur�ck.
 * Falls kein Objekt selektiert ist, wird -1 zur�ckgegeben.
 * @return ID des selektierten Objekts
 */
public long getIdOfSelectedObject () {
	DrawableObject obj = drawableObjects.getSelectedObject();
	if (obj != null)
		return obj.getId();
	else
		return -1;
}
/**
 * Gibt die ID des selektierten Objekts als String zur�ck.
 * Falls kein Objekt selektiert ist, wird einee leere Zeichenkette zur�ckgegeben.
 * @return ID des selektierten Objekz
 */
public String getIdOfSelectedObjectAsString () {
	long id = getIdOfSelectedObject();
	if (id != -1)
		return String.valueOf (id);
	else
		return "";
}
/**
 * Gibt den Info-Text zu einem Objekt zur�ck.
 * Die R�ckgabe von null ist zul�ssig.
 * @return Info-Text
 * @param obj Objekt
 */
protected abstract String getInfoText (DrawableObject obj);
/**
 * Anlegen / Zur�ckgeben des Name-Labels.
 * @return Name-Label
 */
protected Label getNameLabel() {
	if (ivjNameLabel == null) {
		ivjNameLabel = new Label();
		ivjNameLabel.setName("NameLabel");
		ivjNameLabel.setFont(new Font("sansserif", 0, 12));
		ivjNameLabel.setText("...");
	};
	return ivjNameLabel;
}
/**
 * Returns the name of the selected object.
 * @return Symbolname
 */
public String getNameOfSelectedObject () {
	DrawableObject obj = drawableObjects.getSelectedObject();
	if (obj != null)
		return obj.getName();
	else
		return "";
}
/**
 * Anlegen / Zur�ckgeben des North-Buttons.
 * @return East-Button
 */
protected Button getNorthButton() {
	if (ivjNorthButton == null) {
		ivjNorthButton = new Button();
		ivjNorthButton.setName("NorthButton");
		ivjNorthButton.setFont(new Font("Dialog", 0, 12));
		ivjNorthButton.setLabel("N");
	};
	return ivjNorthButton;
}
/**
 * Gibt Informationen �ber die unterst�tzten Parameter zur�ck.
 * @return Parameter-Info in String-Array.
 */
public java.lang.String[][] getParameterInfo() {
	String[][] info = {
		{"name of parameter", "type of parameter", "see: ShowMapParameters.html"},
	};
	return info;
}
/**
 * Anlegen / Zur�ckgeben des PressInfo-Labels.
 * @return PressInfo-Label
 */
protected Label getPressInfoLabel() {
	if (ivjPressInfoLabel == null) {
		ivjPressInfoLabel = new Label();
		ivjPressInfoLabel.setName("PressInfo");
		ivjPressInfoLabel.setFont(new Font("dialog", 0, 10));
		if (language == GERMAN)
			ivjPressInfoLabel.setText("Aufziehen: Neue Mitte & Gr��er");
		else
			ivjPressInfoLabel.setText("drag: new center & zoom in");
	};
	return ivjPressInfoLabel;
}
/**
 * Anlegen / Zur�ckgeben des Scale-Labels.
 * @return Scale-Label
 */
protected Label getScaleLabel() {
	if (ivjScaleLabel == null) {
		ivjScaleLabel = new Label();
		ivjScaleLabel.setName("ScaleLabel");
		ivjScaleLabel.setAlignment(Label.LEFT);
		ivjScaleLabel.setFont(new Font("Dialog", 0, 12));
		ivjScaleLabel.setText("...");
	};
	return ivjScaleLabel;
}
/**
 * Anlegen / Zur�ckgeben des ShiftClickInfo-Labels.
 * @return ShiftClickInfo-Label
 */
protected Label getShiftClickInfoLabel() {
	if (ivjShiftClickInfoLabel == null) {
		ivjShiftClickInfoLabel = new Label();
		ivjShiftClickInfoLabel.setName("ShiftClickInfoLabel");
		ivjShiftClickInfoLabel.setFont(new Font("dialog", 0, 10));
		if (language == GERMAN)
			ivjShiftClickInfoLabel.setText("Shift-Klick: Neue Mitte");
		else
			ivjShiftClickInfoLabel.setText("shift click: new center");
	};
	return ivjShiftClickInfoLabel;
}
/**
 * Anlegen / Zur�ckgeben des South-Buttons.
 * @return South-Button
 */
protected Button getSouthButton() {
	if (ivjSouthButton == null) {
		ivjSouthButton = new Button();
		ivjSouthButton.setName("SouthButton");
		ivjSouthButton.setLabel("S");
	};
	return ivjSouthButton;
}
/**
 * Gibt Applet-Status zur�ck.
 * @return Status
 */
protected int getState () {
	return state;
}
/**
 * Anlegen / Zur�ckgeben des Tag-Labels.
 * @return Tag-Label
 */
protected Label getTagLabel() {
	if (ivjTagLabel == null) {
		ivjTagLabel = new Label();
		ivjTagLabel.setName("TagLabel");
		ivjTagLabel.setFont(new Font("sansserif", 0, 12));
		ivjTagLabel.setText("...");
	};
	return ivjTagLabel;
}
/**
 * Anlegen / Zur�ckgeben der Unicode-Checkbox.
 * @return Unicode-Checkbox
 */
protected Checkbox getUnicodeCheckbox() {
	if ((ivjUnicodeCheckbox == null) && (unicodeSupported)) {
		ivjUnicodeCheckbox = new Checkbox();
		ivjUnicodeCheckbox.setName("UnicodeCheckbox");
		ivjUnicodeCheckbox.setFont(new Font("dialog", 0, 11));
		ivjUnicodeCheckbox.setLabel("Unicode");
		ivjUnicodeCheckbox.setState(true);
	};
	return ivjUnicodeCheckbox;
}
/**
 * Anlegen / Zur�ckgeben des Value-Labels.
 * @return Value-Label
 */
protected Label getValueLabel() {
	if (ivjValueLabel == null) {
		ivjValueLabel = new Label();
		ivjValueLabel.setName("ValueLabel");
		ivjValueLabel.setFont(new Font("sansserif", 0, 12));
		ivjValueLabel.setText("...");
	};
	return ivjValueLabel;
}
/**
 * Anlegen / Zur�ckgeben des West-Buttons.
 * @return West-Button
 */
protected Button getWestButton() {
	if (ivjWestButton == null) {
		ivjWestButton = new Button();
		ivjWestButton.setName("WestButton");
		ivjWestButton.setFont(new Font("dialog", 0, 12));
		ivjWestButton.setLabel("W");
	};
	return ivjWestButton;
}
/**
 * Anlegen / Zur�ckgeben des ZoomIn-Buttons.
 * @return ZoomIn-Button
 */
protected Button getZoomInButton() {
	if (ivjZoomInButton == null) {
		ivjZoomInButton = new Button();
		ivjZoomInButton.setName("ZoomInButton");
		if (language == GERMAN)
			ivjZoomInButton.setLabel("Gr��er");
		else
			ivjZoomInButton.setLabel("Zoom In");
	};
	return ivjZoomInButton;
}
/**
 * Anlegen / Zur�ckgeben des ZoomOut-Buttons.
 * @return ZoomOut-Button
 */
protected Button getZoomOutButton() {
	if (ivjZoomOutButton == null) {
		ivjZoomOutButton = new Button();
		ivjZoomOutButton.setName("ZoomOutButton");
		if (language == GERMAN)
			ivjZoomOutButton.setLabel("Kleiner");
		else
			ivjZoomOutButton.setLabel("Zoom Out");
	};
	return ivjZoomOutButton;
}
/**
 * Initialisieren des Applets.
 */
public void init() {
	super.init();
	setName("ShowMap");
	// �bergebene Parameter auswerten
	interpretParameters();
	// Oberfl�chenelemente hinzuf�gen
	setLayout(null);
	setBackground (backgroundColor);
	setSize (2*viewX+viewWidth, viewY+viewHeight+panelHeight);
	if (panelY >= 0)
		panelY = viewY+viewHeight;
	else {
		panelY = 0;
		viewY = panelHeight;
	}
	addComponentsToApplet();
	addComponentsToListeners();
	changeComponentPositions();
	// Klassen initialisieren
	DrawableBitmap.init(this);
	String p = getParameter("layers");
	if (p != null)
		numOfLayers = new Integer(p).intValue();
	if (drawableObjects == null)
		drawableObjects = new DrawableObjects(numOfLayers);
	initDrawablePresentation();
	// Anzeige vorbereiten
	depictObjectAttributes (null);
	checkViewPoint();
	movePos(-1);
	startLoadingThread ();
}
/**
 * Legt die notwendigen Darstellungsobjekte an.
 */
protected abstract void initDrawablePresentation ();
/**
 * Interpretiert die Parameter des Applets (mit Ausnahme der URLs).
 */
protected void interpretParameters () {
	String p = getParameter("basescalefactor");
	if (p != null)
		baseScaleFactor = new Integer(p).intValue();
	p = getParameter("minscale");
	if (p != null)
		minScale = new Integer(p).intValue();
	p = getParameter("maxscale");
	if (p != null)
		maxScale = new Integer(p).intValue();
	p = getParameter("scale");
	if (p != null)
		scale = new Integer(p).intValue();
	else
		scale = maxScale;
	p = getParameter("viewwidth");
	if (p != null)
		viewWidth = new Integer(p).intValue();
	mapWidth = scale*viewWidth;
	p = getParameter("viewheight");
	if (p != null)
		viewHeight = new Integer(p).intValue();
	mapHeight = scale*viewHeight;
	p = getParameter("mapwidth");
	if (p != null)
		mapWidth = new Integer(p).intValue();
	p = getParameter("mapheight");
	if (p != null)
		mapHeight = new Integer(p).intValue();
	p = getParameter("viewx");
	if (p != null)
		viewX = new Integer(p).intValue();
	p = getParameter("viewy");
	if (p != null)
		viewY = new Integer(p).intValue();
	p = getParameter("unicode");
	if (p != null) {
		unicodeSupported = (p.compareTo("yes")==0);
		DrawableText.setUnicode (true);
	}
	p = getParameter("color");
	if (p != null)
		backgroundColor = ColorDefiner.getColor(p);
	p = getParameter("language");
	if ((p != null) && (p.compareTo("D")==0))
		language = GERMAN;
}
/**
 * Methode zur Behandlung von StateChanged-Events f�r das ItemListener-Interface.
 * @param e akt. Event
 */
public void itemStateChanged (java.awt.event.ItemEvent e) {
	if ((e.getSource() == getUnicodeCheckbox()) ) {
		if (unicodeSupported)
			setUnicode(getUnicodeCheckbox().getState());
	}
}
/**
 * Die Methode wird aufgerufen, falls evtl. Daten nach �nderung des Kartenausschnitts
 * oder �hnlichen Ereignissen geladen werden m�ssen.
 * Standardm��ig passiert nichts.
 */
protected void loadDrawables () {
}
/**
 * Methode zur Behandlung vom mouseClicked-Event f�r das MouseListener-Interface.
 * @param e akt. Event
 */
public void mouseClicked (java.awt.event.MouseEvent e) {
	if ((e.getSource() == this) ) {
		// Falls Shift-Taste gedr�ckt, Position anpassen
		if (e.isShiftDown())
			movePos (e.getX(), e.getY());
		// Sonst: Objekt suchen und ggf. anzeigen
		else {
			DrawableObject oldSelectedObject = drawableObjects.getSelectedObject();
 			DrawableObject obj = findObject (e.getX(), e.getY(), true);
 			if (obj != oldSelectedObject)
 				depictObjectAttributes (obj);
		}
	}
}
/**
 * Methode zur Behandlung vom mouseDragged-Event f�r das MouseMotionerListener-Interface.
 * @param e akt. Event
 */
public void mouseDragged (MouseEvent e) {
	if ((e.getSource() == this) ) {
		paintDragBox (lastMouseXPos, lastMouseYPos, lastDragXPos,lastDragYPos);	// alte Box l�schen
		lastDragXPos = e.getX();
		lastDragYPos = e.getY();
		paintDragBox (lastMouseXPos, lastMouseYPos, lastDragXPos,lastDragYPos);	// neue Box zeichnen
	}
}
/**
 * Methode zur Behandlung vom mouseEntered-Event f�r das MouseListener-Interface.
 * @param e akt. Event
 */
public void mouseEntered (java.awt.event.MouseEvent e) {
}
/**
 * Methode zur Behandlung vom mouseExited-Event f�r das MouseListener-Interface.
 * @param e akt. Event
 */
public void mouseExited (java.awt.event.MouseEvent e) {
}
/**
 * Methode zur Behandlung vom mouseMoved-Event f�r das MouseMotionerListener-Interface.
 * @param e akt. Event
 */
public void mouseMoved (MouseEvent e) {
	if (e.getSource() == this) {
		DrawableObject obj = findObject (e.getX(), e.getY(), true);
		// ggf. Info-Text entfernen
 		if (obj == null) {
 			if (infoIsShown) {
	 			paint (this.getGraphics());
	 			infoIsShown = false;
	 		}
		}
		// ggf. Info-Text neu zeichnen
		else if (! infoIsShown) {
			paintInfo (e.getX()+10, e.getY()-10, obj);
			infoIsShown = true;
		}
	}
}
/**
 * Methode zur Behandlung vom mousePressed-Event f�r das MouseListener-Interface.
 * @param e akt. Event
 */
public void mousePressed (MouseEvent e) {
	if ((e.getSource() == this) ) {
		lastMouseXPos = e.getX();
		lastMouseYPos = e.getY();
		lastDragXPos = e.getX();
		lastDragYPos = e.getY();
		setCursor (Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}
}
/**
 * Methode zur Behandlung vom mouseReleased-Event f�r das MouseListener-Interface.
 * @param e akt. Event
 */
public void mouseReleased (java.awt.event.MouseEvent e) {
	if ((e.getSource() == this) ) {
		// DragBox l�schen
		paintDragBox (lastMouseXPos, lastMouseYPos, lastDragXPos,lastDragYPos);
		// Falls die Maus sich hinreichend bewegt hat und Applet aktiv, Zoom und Move
		if ( ((Math.abs(lastMouseXPos-e.getX()) > 3) || (Math.abs(lastMouseYPos-e.getY()) > 3)) &&
		     (state >= ACTIVE) ) {
			viewMapX = viewMapX-viewWidth/2+(lastMouseXPos+e.getX())/2-viewX;
			viewMapY = viewMapY-viewHeight/2+(lastMouseYPos+e.getY())/2-viewY;
			if (scale == maxScale) {
				checkViewPoint();
				repaint();
				loadDrawables();
			}
			else
				zoomIn();
		}
		setCursor (Cursor.getDefaultCursor());
	}
}
/**
 * Kartenausschnitt nach Osten verschieben.
 */
public void moveEast () {
	int oldViewMapX = viewMapX;
	viewMapX = viewMapX + viewWidth/10*7;
	checkViewPoint ();
	if (oldViewMapX != viewMapX) {
		repaint();
		loadDrawables();
	}
}
/**
 * Kartenausschnitt nach Norden verschieben.
 */
public void moveNorth () {
	int oldViewMapY = viewMapY;
	viewMapY = viewMapY - viewHeight/10*7;
	checkViewPoint ();
	if (oldViewMapY != viewMapY) {
		repaint();
		loadDrawables();
	}
}
/**
 * Verschiebt Mittelpunkt des Kartenausschnitts auf (x,y).
 * @param x x-Koordinate in Applet-Pixel
 * @param y y-Koordinate in Applet-Pixel
 */
public void movePos (int x, int y) {
	int oldViewMapX = viewMapX;
	int oldViewMapY = viewMapY;
	viewMapX = viewMapX-viewWidth/2+x-viewX;
	viewMapY = viewMapY-viewHeight/2+y-viewY;
	checkViewPoint ();
	if ((oldViewMapX != viewMapX) || (oldViewMapY != viewMapY)) {
		repaint();
		loadDrawables();
	}
}
/**
 * Verschiebt Mittelpunkt des Kartenausschnitts auf (x,y) und
 * setzt Ma�stab auf den kleinsten Ma�stab, der gr��ergleich s ist.
 * @param x x-Koordinate in Welt-Koordinaten
 * @param y y-Koordinate in Welt-Koordinaten
 * @param s Ma�stab
 */
public void movePos (int x, int y, int s) {
	scale = minScale;
	if (s < maxScale)
		s = maxScale;
	while (scale > s)
		scale = scale / 2;
	viewMapX = x / scale;
	viewMapY = y / scale;
	DrawableObject obj = findObject (viewX+viewWidth/2, viewY+viewHeight/2, true);
	depictObjectAttributes (obj);
	checkViewPoint ();
	repaint();
	loadDrawables();
}
/**
 * Setzt des Kartenausschnitt so, da� sich das Symbol mit der �bergebenen ID
 * in der Ausschnittsmitte befindet. Au�erdem werden die Attribute des Symbols
 * dargestellt und das Symbol selektiert.
 * @param id ID des Symbols
 */
public void movePos (long id) {
	DrawableObject obj = drawableObjects.getObjectById (id);
	if (obj != null) {
		Drawable prim = obj.getDrawable(0);
		scale = minScale;
		int s = obj.getDefaultScale();
		if (s < maxScale)
			s = maxScale;
		while (scale > s)
			scale = scale / 2;
		Rectangle mbr = prim.getMBR();
		viewMapX = (mbr.x+mbr.width/2) / scale;
		viewMapY = (mbr.y+mbr.height/2) / scale;
		checkViewPoint ();
		obj = drawableObjects.getVisibleObjectById (id,scale);
	}
	depictObjectAttributes (obj);
	repaint();
	loadDrawables();
}
/**
 * Kartenausschnitt nach S�den verschieben.
 */
public void moveSouth () {
	int oldViewMapY = viewMapY;
	viewMapY = viewMapY + viewHeight/10*7;
	checkViewPoint ();
	if (oldViewMapY != viewMapY) {
		repaint();
		loadDrawables();
	}
}
/**
 * Kartenausschnitt nach Westen verschieben.
 */
public void moveWest () {
	int oldViewMapX = viewMapX;
	viewMapX = viewMapX - viewWidth/10*7;
	checkViewPoint ();
	if (oldViewMapX != viewMapX) {
		repaint();
		loadDrawables();
	}
}
/**
 * Draw the double buffer (or the map if the buffer does not exist).
 * @param g graphical context.
 */

public void paint (Graphics g) {
	if (doubleBuffer == null)
		update (g);
	else
		g.drawImage (doubleBuffer, viewX,viewY, this);
}
/**
 * Zeichnet die Drag-Box, falls sie hinreichend gro� ist.
 * @param x1 x-Koordinate 1
 * @param y1 y-Koordinate 1
 * @param x2 x-Koordinate 2
 * @param y2 y-Koordinate 2
 */
protected void paintDragBox (int x1, int y1, int x2, int y2) {
	if ( (x1 != x2) && (y1 != y2)) {
		Graphics g = getGraphics();
		g.setColor (Color.black);
 		g.setXORMode (Color.white);
		g.setClip (viewX,viewY,viewWidth,viewHeight);
		g.drawRect (Math.min(x1,x2),Math.min(y1,y2), Math.abs(x2-x1), Math.abs(y2-y1));
		g.setPaintMode ();
	}
}
/**
 * Stellt einen Info-Text zum �bergebenen Objekt dar.
 * @param x x-Position
 * @param y y-Position
 * @param symb Symbol
 */
protected void paintInfo (int x, int y, DrawableObject obj) {
	String infoText = getInfoText(obj);
	if (infoText == null)
		return;
	Graphics g = this.getGraphics();
	Font f = DrawableText.setFont (g,DrawableText.NORMAL,9);
	// FontMetrics bestimmen
	FontMetrics fm = g.getFontMetrics (f);
	int width = fm.stringWidth (infoText);
	int height = fm.getLeading() + fm.getAscent();
	// Info ausgeben
	g.setClip (viewX+1,viewY+1,viewWidth-2,viewHeight-2);
 	g.setColor (Color.yellow);
 	g.fillRect (x,y-height-3, width+5, height+4);
	g.setColor (Color.black);
 	g.drawRect (x,y-height-3, width+5, height+4);
	g.drawString (infoText, x+3,y-2);
}
/**
 * Wertet URL aus; weiteres siehe readDrawables (int,EntryInput,String).
 * @return Anzahl eingelesener Objekte
 * @param objNum Anzahl bisher eingelesener Objekte
 * @param url URL, wo die zu lesenden Daten liegen
 * @param index Index der URL
 */
protected int readDrawables (int objNum, URL url, int index) {
	try {
		if (language == GERMAN)
			showStatus("�ffne URL ...");
		else
			showStatus("open URL ...");
		// Stream �ffnen
		InputStream is = null;
		EntryInput ber;
		if (url.toString().endsWith(".zip")) {
			ZipInputStream zis = new ZipInputStream(url.openStream());
			zis.getNextEntry();
			is = zis;
			ber = new DataReader(zis);
		}
		else {
			is = url.openStream();
			ber	= new DataReader(is);	// hier besser puffern!!!
		}
		// alle Objekte einlesen
		objNum = readDrawables (objNum,ber);
		is.close();
		return objNum;
	}
	catch (IOException ioe) {
		System.err.println("ShowMap.readDrawables.IOException: " + ioe);
		return objNum;
	}
}
/**
 * Liest Drawable-Datei vom Enry-Input und erzeugt entsprechende Drawable-Objekte.
 * @return Anzahl eingelesener Objekte
 * @param objNum Anzahl bisher eingelesener Objekte
 * @param ber EntryInput
 */
protected int readDrawables (int objNum, EntryInput ber) {
	// read and test file type
	char t1 = ber.readChar();
	char t2 = ber.readChar();
	if ((t1=='D') && (t2=='O')) {
		// read and test version
		int version = ber.readInt();
		if (version < 200) {
			System.err.println("Wrong version!");
			return objNum;
		}
		// read map properties
		if (version >= 210) {
			mapWidth = ber.readInt();
			mapHeight = ber.readInt();
			baseScaleFactor = ber.readInt();
		}
		// read objects
		String objType = null;
		while ((objType = ber.readString()) != null) {
			// commands
			if (objType.startsWith("END"))
				break;
			else if (objType.startsWith("STARTED"))
				state = STARTED;
			else if (objType.startsWith("ACTIVE")) {
				state = ACTIVE;
				setViewToPrefinedValue();
				checkViewPoint();
				update (getGraphics());
			}
			// object type
			else if (objType.compareTo("ObjectType")==0)
				(new DrawableObjectType()).read(ber);
			// object (version >= 210)
			else if (objType.equals("O")) {
				DrawableObject d = drawableObjects.readDrawableObject (ber,version);
				if (d == null) {
					System.err.println ("Read error at object "+(objNum+1));
					break;
				}
			}
			// primitive (version >= 210)
			else if (version >= 210) {
				Drawable d = Drawable.readDrawable (ber,objType);
				if (d == null) {
					System.err.println ("Read error at primitive "+(objNum+1));
					break;
				}
				else {
					drawableObjects.addDrawable(d);
					if (!ber.eol()) {
						long objID = ber.readInt();
						if (objID != 0) {
							DrawableObject obj = drawableObjects.getObjectById(objID);
							if (obj == null)
								drawableObjects.newDrawableObject(objID,"","","").addDrawable(d);
							else
								obj.addDrawable(d);
						}
					}
				}
			}
			// object with primitive (version 200)
			else
				drawableObjects.readDrawableObject (ber,objType);
			objNum++;
			// outputs
			if ((state >= ACTIVE) && (updateTimer.get()>UPDATETIME))
				update (getGraphics());
			if (objNum % 10 == 0)
				if ((state >= ACTIVE) && (language == GERMAN))
					showStatus(objNum+" Objekte geladen; man kann bereits arbeiten ...");
				else if (state >= ACTIVE)
					showStatus(objNum+" objects loaded; you can already work ...");
				else if (language == GERMAN)
					showStatus(objNum+" Objekte geladen... ");
				else
					showStatus(objNum+" objects loaded... ");
		}
	}
	else
		System.err.println("Wrong file type!");
	// return number of objects
	return objNum;
}
/**
 * �ndert die Gr��e der Karte und des Applets.
 * @param pViewWidth Breite der Kartenanzeige
 * @param pViewHeight H�he der Kartenanzeige
 */
public void setMapSize (int pViewWidth, int pViewHeight ) {
	viewWidth = pViewWidth;
	viewHeight = pViewHeight;
	doubleBuffer = null;
	setSize (viewWidth+2*viewX, viewHeight+viewY+163);
	changeComponentPositions();
	repaint();
}
/**
 * Sets the state of the applet.
 * @param state the new state
 */
protected void setState (int state) {
	this.state = state;
	if (state == COMPLETE)
		if (language == GERMAN)
			showStatus ("Bereit!");
		else
			showStatus ("Ready!");
}
/**
 * Stellt die Text-Ausgabe in Abh�ngigkeit vom Parameter auf Unicode oder ANSI.
 * @param f Unicode?
 */
protected void setUnicode (boolean f) {
	DrawableText.setUnicode (f);
	repaint();
}
/**
 * Sets the viewpoint to the value predefined by the parameters.
 */
public void setViewToPrefinedValue() {
	String p = getParameter("posx");
	if (p != null)
		viewMapX = new Integer(p).intValue() / scale;
	else
		viewMapX = mapWidth / 2 / scale;
	p = getParameter("posy");
	if (p != null)
		viewMapY = new Integer(p).intValue() / scale;
	else
		viewMapY = mapHeight / 2 / scale;
}
/**
 * Interprets the parameter "url" and starts the loading thread.
 */
protected void startLoadingThread () {
	String urlName = getParameter("url");
	if (urlName != null) {
System.out.println("ShowMap: URL-Name: "+urlName);
		URL url = computeURL (urlName);
System.out.println("ShowMap: URL: "+urlName);
		if (url != null)
			new LoadDrawables (this,url,0).start();
	}
	else
		System.err.println("ShowMap: no URL!");
}
/**
 * Paints the map.
 * @param pg graphical context.
 */

public synchronized void update (Graphics pg) {
	if (state < ACTIVE)
		return;
	// Werte in lokale Variable sichern
	int vMapX = viewMapX;
	int vMapY = viewMapY;
	int sc = scale;
	// Timer zur�cksetzen
	updateTimer.reset();
	// selektiertes Objekt bestimmen
	DrawableObject oldSelectedObject = drawableObjects.getSelectedObject();
	drawableObjects.deselect();
	// ggf. Double-Buffer anlegen
	if (doubleBuffer == null)
		doubleBuffer = createImage (viewWidth, viewHeight);
  	Graphics g = doubleBuffer.getGraphics();
	// Hintergrund zeichnen
 	g.setPaintMode ();
	g.setClip (0,0, viewWidth,viewHeight);
	g.setColor (backgroundColor);
	g.fillRect (0,0,viewWidth,viewHeight);
	// Karte zeichnen
	g.translate (-vMapX+viewWidth/2,-vMapY+viewHeight/2);
	g.setClip (vMapX-viewWidth/2,vMapY-viewHeight/2,viewWidth+1,viewHeight+1);
	g.setColor (mapColor);	// Wasser
	g.fillRect (0,0,mapWidth/scale,mapHeight/scale);
	Rectangle r = new Rectangle ((vMapX-viewWidth/2)*sc,(vMapY-viewHeight/2)*sc, viewWidth*sc,viewHeight*sc);
	drawMap (g,r,sc);
	// selektiertes Objekt darstellen
	if (oldSelectedObject != null) {
		drawableObjects.select (oldSelectedObject);
		oldSelectedObject.draw(g,r,sc);
	}
	g.translate (vMapX-viewWidth/2,vMapY-viewHeight/2);
	g.setClip (0,0, viewWidth,viewHeight);
	g.setColor (Color.black);
	g.drawRect (0,0, viewWidth-1,viewHeight-1);
	// Ma�stab setzen
	getScaleLabel().setText("1 : "+String.valueOf(sc*baseScaleFactor));
	// Darstellung sichtbar machen
	pg.drawImage (doubleBuffer, viewX,viewY, this);
	// Timer starten
	updateTimer.start();
}
/**
 * Transforms pixel position into x-coordinate.
 * @return x-coordinate
 * @param x x-pixel
 */
public int xIntoCoord (int x) {
	x = x-viewX+viewMapX-(viewWidth/2);
	return x*scale;
}
/**
 * Transforms pixel position into y-coordinate.
 * @return y-coordinate
 * @param y y-pixel
 */
public int yIntoCoord (int y) {
	y = y-viewY+viewMapY-(viewHeight/2);
	return y*scale;
}
/**
 * Zoomt in Karte hinein.
 */
public void zoomIn () {
	if (scale == maxScale)
		return;
	scale = scale / 2;
	viewMapX = viewMapX * 2;
	viewMapY = viewMapY * 2;
	checkViewPoint ();
	repaint ();
	loadDrawables();
}
/**
 * Zoomt aus der Karte heraus.
 */
public void zoomOut () {
	if (scale == minScale)
		return;
	scale = scale * 2;
	viewMapX = viewMapX / 2;
	viewMapY = viewMapY / 2;
	checkViewPoint ();
	repaint ();
	loadDrawables();
}
}
