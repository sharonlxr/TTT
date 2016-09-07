package com.freescale.stateStamper.view;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

/**
 * This class displays a transition state chart
 */

public class StateTransitions extends JPanel
	{
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private JLabel toolTip;
	private Hashtable data, colors, states = new Hashtable();
	private String[] contexts, summings, machines;
	private Date startDate, endDate;
	private float maxValue;
	private int leftMargin = 16, topMargin = 8, rightMargin = 16, bottomMargin, widthLegendItem;
	
	/**
	 * Displays a transition state chart
	 * @param data A hashtable with machines and a vector of state and duration for each
	 * @param lines An array of keys that will be displayed as lines instead of bars
	 * @param colors A hastable with state colors
	 * @param startTime The start time for the report
	 * @param endTime The end time for the report
	 */
	public StateTransitions(Hashtable data, String[] auxContexts, String[] auxSummings, Hashtable colors, Date startDate, Date endDate)
		{
		this.data = data;
		contexts = auxContexts;
		summings = auxSummings;
		this.startDate = startDate;
		this.endDate = endDate;
		String endTime = formatter.format(endDate);
		boolean legend = false;
		if (contexts == null)
			{
			legend = true;
			Vector keys = new Vector(data.keySet());
			Collections.sort(keys);
			contexts = new String[keys.size()];
			summings = new String[]{};
			for (int i=0; i<keys.size(); i++) contexts[i] = (String)keys.elementAt(i);
			}
		machines = new String[contexts.length+summings.length];
		for (int m=0; m<machines.length; m++) machines[m] = m<contexts.length?contexts[m]:summings[m-contexts.length];
		for (int m=0; m<machines.length; m++)
			{
			String machine = machines[m];
			if (machine.length()*7+8 > leftMargin) leftMargin = machine.length()*7+8;
			Vector elements = (Vector)data.get(machine), newElements = new Vector();
			for (int i=0; i<elements.size(); i++)
				{
				String[] items = ((String)elements.elementAt(i)).split("\t");
				String state = items[0], time = items[1], duration = items[2];	
				if (state.equals("")) state = "Unknown";
				if (legend) states.put(state,"");
				process(newElements,state,time,duration,startDate);
				}
			process(newElements,null,endTime,null,null);
			data.put(machine,newElements);
			long total = 0;
			for (int i=0; i<newElements.size(); i+=2) total += ((Long)newElements.elementAt(i+1)).longValue();
			if (total > maxValue) maxValue = total;
			}
		this.colors = (Hashtable)colors.clone();
		setOpaque(false);
		addMouseMotionListener(new MouseMotionAdapter()
			{
			public void mouseMoved(MouseEvent event) {processEvent(event);}
			});
		toolTip = new JLabel();
		toolTip.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		toolTip.setOpaque(true);
		toolTip.setBackground(Color.WHITE);
		add(toolTip);
		setPreferredSize(new Dimension(700,500));
		}
	
	/**
	 * Paint chart
	 * @param graphics The graphics object
	 */
	protected void paintComponent(Graphics graphics) 
		{	
		Dimension dim = getSize();
		bottomMargin = calculateBottomMargin(dim.width,dim.height);
		int width = dim.width-leftMargin-rightMargin, height = dim.height-topMargin-bottomMargin;
		float h = height/(machines.length+0.2F*machines.length-0.2F), y = topMargin;
		for (int m=0; m<machines.length; m++)
			{
			String machine = machines[m];
			boolean summing=false;
			for (int j=0; j<summings.length; j++) if (machine.equals(summings[j])) summing=true;
			Vector values = (Vector)data.get(machine);
			long totalDuration = 0;
			graphics.setColor(Color.BLACK);
			graphics.drawString(Machines.deCapitalize(machine),8,(int)(y+h/2+4));
			for (int j=0; j<values.size(); j+=2)
				{
				String state = (String)values.elementAt(j);
				long duration = ((Long)values.elementAt(j+1)).longValue();
				int x = leftMargin + Math.round(totalDuration*width/maxValue), w = Math.round(duration*width/maxValue)+1;
				if (w == 0) w = 1;
				int auxH = (int)h, auxY = (int)y;
				if (summing)
					{
					try {auxH = (int)(h*Integer.parseInt(state)/100); auxY +=(int)h-auxH;}
					catch (Throwable t) {auxH=0;}
					}
				drawBar(graphics,state,x,auxY,w,auxH);
				totalDuration += duration;
				}		
			y += 1.2*h;
			}
		drawTimeAxis(graphics,width,height);
		drawLegend(graphics,width,height);
		}
	
	/**
	 * Adds a transition to vector and adds unknown time if needed
	 * @param vector A vector with a list of states and durations
	 * @param state The state for this transition
	 * @param time The last time for the next state
	 * @param duration The duration for this transition
	 * @param startDate The initial date for the report
	 */
	private void process(Vector vector, String state, String time, String duration, Date startDate)
		{
		long prevDuration = 0; 
		String prevState = "", prevTime;
		int size = vector.size();
		if (size == 0) prevTime = formatter.format(startDate);
		else
			{ // Get previous state, duration and time
			prevState = (String)vector.elementAt(size-3);
			prevDuration = ((Long)vector.elementAt(size-2)).longValue();
			prevTime = (String)vector.elementAt(size-1);
			vector.removeElementAt(size-1); // Remove last time from the vector
			}
		long diff = diff(time,prevTime);
		// If time is before previous time it is an event that started before the start date. 
		// else if duration of previous state and time difference do not match, add unknown state for the difference
		// else if duration is greater than the time difference, trim it
		if (diff < 0)
			{
			if (-diff*1000-Float.parseFloat(duration) > 0) 
			{
			if (size !=0) vector.addElement(prevTime); // Add back the removed Time if the vector is not empty.
			return; // Return if this first event does not go beyond the start date
			}
			duration = ""+((Float.parseFloat(duration)+diff*1000)); // Trim duration
			time = prevTime; // Set time to start date			
			}
		else if (diff*1000-prevDuration > 1000)
			{
			vector.addElement("Unknown");
			vector.addElement(new Long(diff*1000-prevDuration));
			prevState = ""; // Do not merge the previous state since Unknown is added
			}
		else if (prevDuration-diff*1000 > 1000) vector.setElementAt(new Long(diff*1000),size-2);
		// Add state, duration and time to the vector. Time will be removed when the next record is processed
		if (state != null)
			{
			if (prevState.equals(state))
				{			
				vector.setElementAt(new Long(prevDuration+(long)Float.parseFloat(duration)),size-2); // Do not add new state since it is equals than previous. Add the durations
				vector.addElement(prevTime);
				}
			else
				{
				vector.addElement(state);
				vector.addElement(new Long((long)Float.parseFloat(duration)));
				vector.addElement(time);
				}		
			}
		}
		
	/**
	 * Calculate the height of the bottom margin to display the state color legend
	 * @param width The width of the display area
	 * @param height The height of the display area
	 * @return The height of the bottom margin
	 */
	private int calculateBottomMargin(int width, int height)
		{
		int maxLength=0;
		for (Enumeration e = states.keys(); e.hasMoreElements();)
			{
			int length=((String)e.nextElement()).length();
			if (length>maxLength) maxLength=length;
			}
		widthLegendItem = maxLength*6+16;  // Each character is 6 pixels plus room for the color rectangle
		int numberX = width/widthLegendItem;
		widthLegendItem = (width-8)/numberX;
		int numberY = (int)Math.ceil (states.size()/(float)numberX);
		return numberY*20+18; // 20 is height of legend row, 18 is to accomodate the horizontal time axis 
		}
	
	/**
	 * Displays the horizontal axis with the time ticks
	 * @param graphics The graphics object
	 * @param width The width of the drawing area
	 * @param height The height of the drawing area
	 */
	private void drawTimeAxis(Graphics graphics, int width, int height)
		{
		graphics.setColor(Color.BLACK);
		graphics.drawLine(leftMargin,height+topMargin,leftMargin+width,height+topMargin);	
		graphics.drawLine(leftMargin,topMargin,leftMargin,height+topMargin);
		float numberHours=(endDate.getTime()-startDate.getTime())/3600000.0F;
  		int hoursPerTick;
  		if (numberHours <= 12) hoursPerTick=1;
  		else if (numberHours <= 24) hoursPerTick=2;
		else if (numberHours <= 48) hoursPerTick=4;
		else if (numberHours <= 96) hoursPerTick=8; 
		else hoursPerTick=24;
		float numberPixelsPerTick=(width/numberHours)*hoursPerTick;
		Calendar calendar=Calendar.getInstance();
		calendar.setTime(startDate);
		int hour=calendar.get(Calendar.HOUR_OF_DAY), minute=calendar.get(Calendar.MINUTE), second=calendar.get(Calendar.SECOND);
   		// Calculate the first hour to be displayed
		int tickHour=(int)(Math.ceil((hour+minute/60F+second/3600F)/hoursPerTick))*hoursPerTick;
		float firstTick=(float)((tickHour*3600-(hour*3600+minute*60+second))/3600.0)/hoursPerTick;
		if (numberPixelsPerTick == 0) return;
		for (float tickX=leftMargin+firstTick*numberPixelsPerTick; tickX<=leftMargin+width; tickX+=numberPixelsPerTick)
			{
   			if (tickHour >= 24) tickHour-=24;
   			graphics.drawLine((int)tickX,height+topMargin-3,(int)tickX,height+topMargin+3);
	      	String label=tickHour+":00";
	      	graphics.drawString(label,(int)(tickX-label.length()*3),height+topMargin+15);
			tickHour+=hoursPerTick;
			}
		}
	
	/**
	 * Draw bar for a data item
	 * @param graphics Graphics object
	 * @param state Data item
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @param w Bar width
	 * @param h Bar height
	 */
	private void drawBar(Graphics graphics, String state, int x, int y, int w, int h)
		{
		Color color1 = (Color)colors.get(state);
		if (color1 == null) color1 = new Color(0x808080);
		Color color2 = color1.darker().darker();
		((Graphics2D)graphics).setPaint(new GradientPaint(x,y,color1,x,y+h,color2,true));
		graphics.fillRect(x,y,w,h);
		}
	
	/**
	 * Draws the legend with state and colors
	 * @param graphics The graphics object
	 * @param width The width of the drawing area
	 * @param height The height of the drawing area
	 */
	private void drawLegend(Graphics graphics, int width, int height)
		{
		int x=8, y=height+topMargin+18; // 18 is for the horizontal time axis
		Vector keys = new Vector(states.keySet());
		Collections.sort(keys);
		for (int i=0; i<keys.size(); i++)
			{
			String state=(String)keys.elementAt(i);
			drawBar(graphics,state,x,y,16,16);
			graphics.setColor(Color.black);
			graphics.drawString(state,x+17,y+12);
			x+=widthLegendItem;
			if (x >= width+leftMargin+rightMargin-20) {x=8; y+=20;} // 20 is height of legend row 
			}
		}
	
	/**
	 * Process the mouse move events
	 * @param event Mouse move events
	 */
	private void processEvent(MouseEvent event)
		{
		event.consume();
		int mouseX = event.getX(), mouseY = event.getY();
		Dimension dim = getSize();
		bottomMargin = calculateBottomMargin(dim.width,dim.height);
		int width = dim.width-leftMargin-rightMargin, height = dim.height-topMargin-bottomMargin;
		float h = height/(machines.length+0.2F*machines.length-0.2F), y = topMargin;
		for (int m=0; m<machines.length; m++)
			{
			String machine = machines[m];
			Vector values = (Vector)data.get(machine);
			long totalDuration = 0;
			for (int j=0; j<values.size(); j+=2)
				{
				String state = (String)values.elementAt(j);
				long duration = ((Long)values.elementAt(j+1)).longValue();
				int x = leftMargin + Math.round(totalDuration*width/maxValue), w = Math.round(duration*width/maxValue)+1;
				if (w == 0) w = 1;
				if (mouseX>=x && mouseX<=x+w && mouseY>=y && mouseY<=y+h)
					{
					toolTip.setText(state);
					toolTip.setLocation(mouseX+16,mouseY);			
					return;
					}
				totalDuration += duration;
				}		
			y += 1.2*h;
			}	
		}
	
	/** 
	 * Calculate the difference in seconds between two dates
	 * @param time2 Last date in format yyyy-MM-dd HH:mm:ss
	 * @param time1 First date in format yyyy-MM-dd HH:mm:ss
	 * @return The time difference is seconds
	 */
	public static long diff(String time2, String time1)
		{	
		if (time2.regionMatches(false,0,time1,0,10))
			{
			int h2,m2,s2,h1,m1,s1;
			h2=Integer.parseInt(time2.substring(11,13));
			m2=Integer.parseInt(time2.substring(14,16));
			s2=Integer.parseInt(time2.substring(17,19));

			h1=Integer.parseInt(time1.substring(11,13));
			m1=Integer.parseInt(time1.substring(14,16));
			s1=Integer.parseInt(time1.substring(17,19));
			
			return ((h2-h1)*3600+(m2-m1)*60+(s2-s1));
			}
		else
			{
      		return ((formatter.parse(time2,new ParsePosition(0)).getTime() - formatter.parse(time1,new ParsePosition(0)).getTime())/1000);		
			}
    	}
	}