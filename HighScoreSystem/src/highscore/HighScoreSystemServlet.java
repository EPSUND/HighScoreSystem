package highscore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

@SuppressWarnings("serial")
public class HighScoreSystemServlet extends HttpServlet {
	private static final String[] highScoreLists = new String[]{"breakout", "columns", "etris", "wordonword_swe", "wordonword_eng", "wordonword_ger", "ballbonanza"};
	private static final Logger logger = Logger.getLogger(HighScoreSystemServlet.class.getName());
	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		prepareHighScoreResponse(req, resp);
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		prepareHighScoreResponse(req, resp);
	}
	
	private void addOldEntries()
	{
		extractHighScoreEntries("https://sites.google.com/site/esundholm/test/breakoutHighScore.txt");
		extractHighScoreEntries("https://sites.google.com/site/esundholm/test/columnsHighScore.txt");
		extractHighScoreEntries("https://sites.google.com/site/esundholm/test/wordonwordHighScore.txt");
	}
	
	private void prepareHighScoreResponse(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String highScoreList = req.getParameter("highScoreList");
		
		resp.setContentType("text/plain");
		
		String addOldEntries = req.getParameter("addOldEntries");
		
		//Add old high score entries
		if(addOldEntries != null && addOldEntries.equals("t"))
		{
			addOldEntries();
			resp.getWriter().println("The old high score entries have been added");
		}
		
		//A high score list must be chosen
		if(highScoreList == null)
		{
			resp.getWriter().println("You need to pick a high score list");
			return;
		}
		
		boolean highScoreListExists = false;
		
		for(String hList : highScoreLists)
		{
			if(highScoreList.equals(hList))
			{
				highScoreListExists = true;
				break;
			}
		}
		
		//The selected high score list must exist in the system
		if(!highScoreListExists)
		{
			resp.getWriter().println("The high score list " + highScoreList + " does not exist");
			return;
		}
		
		//Add a potential high score entry to the datastore
		if(highScoreList.equals("breakout"))
		{
			handleBreakoutHighScoreMessage(req);
		}
		else if(highScoreList.equals("columns"))
		{
			handleColumnsHighScoreMessage(req);
		}
		else if(highScoreList.equals("etris"))
		{
			handleEtrisHighScoreMessage(req);
		}
		else if(highScoreList.equals("wordonword_swe") ||
				highScoreList.equals("wordonword_eng") ||
				highScoreList.equals("wordonword_ger"))
		{
			handleWordOnWordHighScoreMessage(req, highScoreList);
		}
		else if(highScoreList.equals("ballbonanza")) {
			handleBallBonanzaHighScoreMessage(req);
		}
		
		//Let the response be the high score list sorted on score
		
	    Key highScoreListKey = KeyFactory.createKey("HighScoreList", highScoreList);
	    
	    Query query = new Query("HighScoreEntry", highScoreListKey).addSort("score", Query.SortDirection.DESCENDING);
	    List<Entity> highScoreEntries = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(25));
	    
	    //Write the high score entries
	    for(Entity highScoreEntry : highScoreEntries)
	    {
	    	Iterator<Map.Entry<String, Object>> iterator = highScoreEntry.getProperties().entrySet().iterator();
	    	
	    	//Iterate over the entry's properties
	    	while(iterator.hasNext())
	    	{
	    		Map.Entry<String, Object> propEntry = iterator.next();
	    		//Write the key and value
	    		resp.getWriter().print(propEntry.getKey() + "=" + propEntry.getValue());
	    		//Write a "," if there are more properties
	    		if(iterator.hasNext())
	    		{
	    			resp.getWriter().print(",");
	    		}
	    	}
	    	//Write a new line before writing the next entity
	    	resp.getWriter().println();
	    }
	}
	
	private void handleBreakoutHighScoreMessage(HttpServletRequest req) throws IOException
	{
		String name = req.getParameter("name");
		String scoreString = req.getParameter("score");
		String levelString = req.getParameter("level");
		String date = req.getParameter("date");
		
		//The request didn't contain a high score entry. We only want to view the list
		if(name == null || scoreString == null || levelString == null || date == null)
		{
			return;
		}
		
		int score, level;
		
		//Parse number values
		try {
			score = Integer.parseInt(scoreString);
			level = Integer.parseInt(levelString);
		} catch (NumberFormatException e) {
			logger.warning("Level and/or score could not be parsed. Not a valid integer.");
			return;
		}
		
		//Add the high score entry to the datastore
		addBreakoutHighScoreEntry(name, score, level, date);
	}
	
	private void addBreakoutHighScoreEntry(String name, int score, int level, String date)
	{
		Key highScoreListKey = KeyFactory.createKey("HighScoreList", "breakout");
		
		Entity highScoreEntry = new Entity("HighScoreEntry", highScoreListKey);
		highScoreEntry.setProperty("name", name);
		highScoreEntry.setProperty("score", score);
		highScoreEntry.setProperty("level", level);
		highScoreEntry.setProperty("date", date);
		
		datastore.put(highScoreEntry);
	}
	
	private void handleColumnsHighScoreMessage(HttpServletRequest req) throws IOException
	{
		String name = req.getParameter("name");
		String scoreString = req.getParameter("score");
		String brickString = req.getParameter("bricks");
		String levelString = req.getParameter("level");
		String timeString = req.getParameter("time");
		String date = req.getParameter("date");
		
		//The request didn't contain a high score entry. We only want to view the list
		if(name == null || scoreString == null || brickString == null || levelString == null || timeString == null || date == null)
		{
			return;
		}
		
		int score, bricks, level, time;
		
		//Parse number values
		try {
			score = Integer.parseInt(scoreString);
			bricks = Integer.parseInt(brickString);
			level = Integer.parseInt(levelString);
			time = Integer.parseInt(timeString);
		} catch (NumberFormatException e) {
			logger.warning("Score, bricks, level or time could not be parsed. Not a valid integer.");
			return;
		}
		
		//Add the high score entry to the datastore
		addColumnsHighScoreEntry(name, score, bricks, level, time, date);
	}
	
	private void addColumnsHighScoreEntry(String name, int score, int bricks, int level, int time, String date)
	{
		Key highScoreListKey = KeyFactory.createKey("HighScoreList", "columns");
		
		Entity highScoreEntry = new Entity("HighScoreEntry", highScoreListKey);
		highScoreEntry.setProperty("name", name);
		highScoreEntry.setProperty("score", score);
		highScoreEntry.setProperty("bricks", bricks);
		highScoreEntry.setProperty("level", level);
		highScoreEntry.setProperty("time", time);
		highScoreEntry.setProperty("date", date);
		
		datastore.put(highScoreEntry);
	}
	
	private void handleEtrisHighScoreMessage(HttpServletRequest req) throws IOException
	{
		String name = req.getParameter("name");
		String scoreString = req.getParameter("score");
		String lineString = req.getParameter("lines");
		String levelString = req.getParameter("level");
		String timeString = req.getParameter("time");
		String date = req.getParameter("date");
		
		//The request didn't contain a high score entry. We only want to view the list
		if(name == null || scoreString == null || lineString == null || levelString == null || timeString == null || date == null)
		{
			return;
		}
		
		int score, lines, level, time;
		
		//Parse number values
		try {
			score = Integer.parseInt(scoreString);
			lines = Integer.parseInt(lineString);
			level = Integer.parseInt(levelString);
			time = Integer.parseInt(timeString);
		} catch (NumberFormatException e) {
			logger.warning("Score, lines, level or time could not be parsed. Not a valid integer.");
			return;
		}
		
		//Add the high score entry to the datastore
		addEtrisHighScoreEntry(name, score, lines, level, time, date);
	}
	
	private void addEtrisHighScoreEntry(String name, int score, int lines, int level, int time, String date)
	{
		Key highScoreListKey = KeyFactory.createKey("HighScoreList", "etris");
		
		Entity highScoreEntry = new Entity("HighScoreEntry", highScoreListKey);
		highScoreEntry.setProperty("name", name);
		highScoreEntry.setProperty("score", score);
		highScoreEntry.setProperty("lines", lines);
		highScoreEntry.setProperty("level", level);
		highScoreEntry.setProperty("time", time);
		highScoreEntry.setProperty("date", date);
		
		datastore.put(highScoreEntry);
	}
	
	private void handleWordOnWordHighScoreMessage(HttpServletRequest req, String highScoreList) throws IOException
	{
		String name = req.getParameter("name");
		String scoreString = req.getParameter("score");
		String wordString = req.getParameter("words");
		String date = req.getParameter("date");
		
		//The request didn't contain a high score entry. We only want to view the list
		if(name == null || scoreString == null || wordString == null || date == null)
		{
			return;
		}
		
		int score, words;
		
		//Parse number values
		try {
			score = Integer.parseInt(scoreString);
			words = Integer.parseInt(wordString);
		} catch (NumberFormatException e) {
			logger.warning("Score or words could not be parsed. Not a valid integer.");
			return;
		}
		
		//Add the high score entry to the datastore
		addWordOnWordHighScoreEntry(name, score, words, date, highScoreList);
	}
	
	private void addWordOnWordHighScoreEntry(String name, int score, int words, String date, String highScoreList)
	{
		Key highScoreListKey = KeyFactory.createKey("HighScoreList", highScoreList);
		
		Entity highScoreEntry = new Entity("HighScoreEntry", highScoreListKey);
		highScoreEntry.setProperty("name", name);
		highScoreEntry.setProperty("score", score);
		highScoreEntry.setProperty("words", words);
		highScoreEntry.setProperty("date", date);
		
		datastore.put(highScoreEntry);
	}
	
	private void handleBallBonanzaHighScoreMessage(HttpServletRequest req) throws IOException
	{
		String name = req.getParameter("name");
		String scoreString = req.getParameter("score");
		String ballString = req.getParameter("balls");
		String timeString = req.getParameter("time");
		String date = req.getParameter("date");
		
		//The request didn't contain a high score entry. We only want to view the list
		if(name == null || scoreString == null || ballString == null || timeString == null || date == null)
		{
			return;
		}
		
		int score, balls, time;
		
		//Parse number values
		try {
			score = Integer.parseInt(scoreString);
			balls = Integer.parseInt(ballString);
			time = Integer.parseInt(timeString);
		} catch (NumberFormatException e) {
			logger.warning("Score, balls or time could not be parsed. Not a valid integer.");
			return;
		}
		
		//Add the high score entry to the datastore
		addBallBonanzaHighScoreEntry(name, score, balls, time, date);
	}
	
	private void addBallBonanzaHighScoreEntry(String name, int score, int balls, int time, String date)
	{
		Key highScoreListKey = KeyFactory.createKey("HighScoreList", "ballbonanza");
		
		Entity highScoreEntry = new Entity("HighScoreEntry", highScoreListKey);
		highScoreEntry.setProperty("name", name);
		highScoreEntry.setProperty("score", score);
		highScoreEntry.setProperty("balls", balls);
		highScoreEntry.setProperty("time", time);
		highScoreEntry.setProperty("date", date);
		
		datastore.put(highScoreEntry);
	}
	
	private void parseHighScoreEntries(InputStream highScoreStream)
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(highScoreStream));
		
		String line;
		String highScoreList = "";
		String[] highScoreData;
		String name, date;
		int level, score, bricks, time, words;
		
		try {
			while((line = reader.readLine()) != null)
			{
				logger.warning(line);
				
				//Get the high score list we are adding entries for
				if(highScoreList == "")
				{
					highScoreList = line;
					continue;
				}
				
				highScoreData = line.split(",");
				
				if(highScoreList.equals("breakout"))
				{
					name = highScoreData[0];
					score = Integer.parseInt(highScoreData[1]);
					level = Integer.parseInt(highScoreData[2]);
					date = highScoreData[3];
					
					addBreakoutHighScoreEntry(name, score, level, date);
				}
				else if(highScoreList.equals("columns"))
				{
					name = highScoreData[0];
					score = Integer.parseInt(highScoreData[1]);
					bricks = Integer.parseInt(highScoreData[2]);
					level = Integer.parseInt(highScoreData[3]);
					time = Integer.parseInt(highScoreData[4]);
					date = highScoreData[5];
					
					addColumnsHighScoreEntry(name, score, bricks, level, time, date);
				}
				else if(highScoreList.equals("wordonword_swe") || highScoreList.equals("wordonword_eng") || highScoreList.equals("wordonword_ger"))
				{
					name = highScoreData[0];
					score = Integer.parseInt(highScoreData[1]);
					words = Integer.parseInt(highScoreData[2]);
					date = highScoreData[3];
					
					addWordOnWordHighScoreEntry(name, score, words, date, highScoreList);
				}
				else//The high score list is not supported
				{
					break;
				}
			}
		}
		catch(NumberFormatException e) {
			logger.warning("Could not parse number");
		}
		catch (IOException e) {
			logger.warning("Could not read high score data");
		}
	}
	
	private void extractHighScoreEntries(String highscoreFileUrl)
	{
		InputStream highScoreStream = null;
		
		try
        {
			//Get an input stream to the high score data
        	URL highScoreURL = new URL(highscoreFileUrl);
        	URLConnection conn = highScoreURL.openConnection();
        	highScoreStream = conn.getInputStream();
        	
			//Parse the highscore entries
        	parseHighScoreEntries(highScoreStream);
        }
        catch(Exception e)
        {
        	logger.warning("Could not get high score data");
        }
		finally
		{
			if(highScoreStream != null)
			{
				try {
					highScoreStream.close();
				} catch (IOException e) {
					logger.warning("Could not close high score input stream");
				}
			}
		}
	}
}
