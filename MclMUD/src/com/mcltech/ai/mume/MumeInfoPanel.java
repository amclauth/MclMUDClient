package com.mcltech.ai.mume;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.mcltech.ai.AIInterface;
import com.mcltech.base.MudLogger;
import com.mcltech.connection.Configger;
import com.mcltech.connection.MudFrame;

public class MumeInfoPanel implements AIInterface
{
   static final MudLogger log = MudLogger.getInstance();
   private boolean running = false;
   private int fontSize;
   Shell shell;
   Display display;
   StyledText roomInfoText;
   StyledText timeText;

   Color RED;
   Color DARK_RED;
   Color BLUE;
   Color DARK_BLUE;
   Color GREEN;
   Color DARK_GREEN;
   Color GRAY;
   Color BLACK;

   final Pattern scPattern = Pattern
         .compile("\\<status>(\\d+)\\<\\/status>\\/\\<status>(\\d+)\\<\\/status> hits?, "
               + "\\<status>(\\d+)\\<\\/status>\\/\\<status>(\\d+)\\<\\/status> mana, and "
               + "\\<status>(\\d+)\\<\\/status>\\/\\<status>(\\d+)\\<\\/status> move");

   boolean isShown = false;

   int hp_low, hp_high, hp_max, hp_wimpy, hp_curr;
   int mv_low, mv_high, mv_max, mv_curr;
   int mana_low, mana_high, mana_max, mana_curr;

   Canvas hpMpManaCanvas;

   private Matcher matcher;

   // singleton class holder pattern
   private static final class holder
   {
      static final MumeInfoPanel INSTANCE = new MumeInfoPanel();
   }

   public static MumeInfoPanel getInstance()
   {
      return holder.INSTANCE;
   }

   public double getHp()
   {
      if (hp_curr == -1)
         return hp_low * 1.0 / hp_max;
      return hp_curr * 1.0 / hp_max;
   }
   
   public double getMv()
   {
      if (mv_curr == -1)
         return mv_low * 1.0 / mv_max;
      return mv_curr * 1.0 / mv_max;
   }
   
   public double getMana()
   {
      if (mana_curr == -1)
         return mana_low * 1.0 / mana_max;
      return mana_curr * 1.0 / mana_max;
   }

   public MumeInfoPanel()
   {
      hp_max = 100;
      mv_max = 100;
      mana_max = 100;
      hp_low = 50;
      hp_high = 70;
      hp_wimpy = 30;
      hp_curr = -1;
      mv_low = 40;
      mv_high = 60;
      mv_curr = -1;
      mana_low = 30;
      mana_high = 80;
      mana_curr = -1;
   }

   public boolean isShown()
   {
      return isShown;
   }

   @Override
   public void trigger(String line)
   {
      if (line.contains("</room>"))
      {
         updateRoomText();
      }
      else if (line.contains("</status> mana, and <status>"))
      {
         matcher = scPattern.matcher(line);
         // if (m.find() && m.groupCount() > 2)
         if (matcher.find() && matcher.groupCount() == 6)
         {
            scoreStatMatch(matcher);
         }
      }
      else if (line.contains("You will flee if your hit points go below <status>"))
      {
         int idx1 = line.indexOf("You will flee if your hit points go below <status>");
         int idx2 = line.indexOf("</status>", idx1 + 50);
         if (idx2 > idx1)
         {
            try
            {
               hp_wimpy = Integer.valueOf(line.substring(idx1 + 50, idx2)).intValue();
               doHpMpManaRedraw();
            }
            catch (@SuppressWarnings("unused")
            NumberFormatException e)
            {
               log.add(Level.WARNING, "Couldn't convert " + line + " to wimpy value.");
            }
         }
      }
      else if (line.contains("Wimpy set to: "))
      {
         int idx1 = line.indexOf("Wimpy set to: ");
         int idx2 = line.trim().length();
         if (idx2 > idx1)
         {
            try
            {
               hp_wimpy = Integer.valueOf(line.substring(idx1 + 14, idx2)).intValue();
               doHpMpManaRedraw();
            }
            catch (@SuppressWarnings("unused")
            NumberFormatException e)
            {
               log.add(Level.WARNING, "Couldn't convert " + line + " to wimpy value.");
            }
         }
      }
      else if (line.contains("<prompt>") && line.contains("</prompt>"))
      {
         hpPromptMatch(line.trim());
      }
   }

   private void hpPromptMatch(String line)
   {
      int hpIdx = line.indexOf("HP:");
      int manaIdx = line.indexOf("Mana:");
      int moveIdx = line.indexOf("Move:");

      int old_hp_high = hp_high;
      int old_hp_low = hp_low;
      if (hpIdx >= 0)
      {
         String substr = line.substring(hpIdx + 3);
         if (substr.startsWith("Fine"))
         {
            hp_high = (int) (hp_max * 0.99);
            hp_low = (int) (hp_max * 0.71);
         }
         else if (substr.startsWith("Hurt"))
         {
            hp_high = (int) (hp_max * 0.70);
            hp_low = (int) (hp_max * 0.46);
         }
         else if (substr.startsWith("Wounded"))
         {
            hp_high = (int) (hp_max * 0.45);
            hp_low = (int) (hp_max * 0.26);
         }
         else if (substr.startsWith("Bad"))
         {
            hp_high = (int) (hp_max * 0.25);
            hp_low = (int) (hp_max * 0.11);
         }
         else if (substr.startsWith("Awful"))
         {
            hp_high = (int) (hp_max * 0.10);
            hp_low = (int) (hp_max * 0.01);
         }
         else if (substr.startsWith("Dying"))
         {
            hp_high = 0;
            hp_low = 0;
         }
      }
      else
      {
         hp_high = hp_max;
         hp_low = hp_max;
         hp_curr = hp_max;
      }

      int old_mana_high = mana_high;
      int old_mana_low = mana_low;
      if (manaIdx >= 0)
      {
         String substr = line.substring(manaIdx + 5);
         if (substr.startsWith("Burning"))
         {
            mana_high = (int) (mana_max * 0.99);
            mana_low = (int) (mana_max * 0.76);
         }
         else if (substr.startsWith("Hot"))
         {
            mana_high = (int) (mana_max * 0.75);
            mana_low = (int) (mana_max * 0.46);
         }
         else if (substr.startsWith("Warm"))
         {
            mana_high = (int) (mana_max * 0.45);
            mana_low = (int) (mana_max * 0.26);
         }
         else if (substr.startsWith("Cold"))
         {
            mana_high = (int) (mana_max * 0.25);
            mana_low = (int) (mana_max * 0.11);
         }
         else if (substr.startsWith("Icy"))
         {
            mana_high = (int) (mana_max * 0.10);
            mana_low = (int) (mana_max * 0.01);
         }
         else if (substr.startsWith("Frozen"))
         {
            mana_high = 0;
            mana_low = 0;
         }
      }
      else
      {
         mana_high = mana_max;
         mana_low = mana_max;
         mana_curr = mana_max;
      }

      int old_mv_high = mv_high;
      int old_mv_low = mv_low;
      if (moveIdx >= 0)
      {
         String substr = line.substring(moveIdx + 5);
         if (substr.startsWith("Tired"))
         {
            mv_high = (int) (mv_max * 0.37);
            mv_low = (int) (mv_max * 0.22);
         }
         else if (substr.startsWith("Slow"))
         {
            mv_high = (int) (mv_max * 0.21);
            mv_low = (int) (mv_max * 0.11);
         }
         else if (substr.startsWith("Weak"))
         {
            mv_high = (int) (mv_max * 0.10);
            mv_low = (int) (mv_max * 0.03);
         }
         else if (substr.startsWith("Fainting"))
         {
            mv_high = (int) (mv_max * 0.02);
            mv_low = (int) (mv_max * 0.01);
         }
         else if (substr.startsWith("Exhausted"))
         {
            mv_high = 0;
            mv_low = 0;
         }
      }
      else
      {
         mv_high = mv_max;
         mv_low = (int) (mv_max * .38);
      }

      if (hp_high != old_hp_high || hp_low != old_hp_low)
      {
         hp_curr = -1;
         doHpMpManaRedraw();
      }
      else if (mv_high != old_mv_high || mv_low != old_mv_low)
      {
         mv_curr = -1;
         doHpMpManaRedraw();
      }
      else if (mana_high != old_mana_high || mana_low != old_mana_low)
      {
         mana_curr = -1;
         doHpMpManaRedraw();
      }
   }

   private void scoreStatMatch(Matcher m)
   {
      try
      {
         int val = Integer.valueOf(m.group(1)).intValue();
         int max = Integer.valueOf(m.group(2)).intValue();
         hp_curr = val;
         hp_max = max;

         val = Integer.valueOf(m.group(3)).intValue();
         max = Integer.valueOf(m.group(4)).intValue();
         mana_curr = val;
         mana_max = max;

         val = Integer.valueOf(m.group(5)).intValue();
         max = Integer.valueOf(m.group(6)).intValue();
         mv_curr = val;
         mv_max = max;

         doHpMpManaRedraw();
      }
      catch (@SuppressWarnings("unused")
      NumberFormatException e)
      {
         log.add(Level.WARNING, "Couldn't convert " + m.group(0) + " to numbers.");
      }
   }

   private void doHpMpManaRedraw()
   {
      display.asyncExec(new Runnable()
      {
         @Override
         public void run()
         {
            hpMpManaCanvas.redraw();
         }
      });
   }

   public void updateRoomText()
   {
      if (roomInfoText != null)
      {
         display.asyncExec(new Runnable()
         {
            List<StyleRange> ranges = new ArrayList<>();

            @Override
            public void run()
            {
               ranges.clear();
               String rname = MumeAI.currentRoom.getName();
               String exits = MumeAI.currentRoom.getExitsString();
               roomInfoText.setText(rname + "\n" + (exits == null ? "" : exits));
               MumeFormatter.formatExits(MumeAI.currentRoom.getExitsString(), ranges);
               int len = MumeAI.currentRoom.getName().length() + 1;
               for (StyleRange r : ranges)
               {
                  r.start += len;
               }
               for (StyleRange r : ranges)
               {
                  roomInfoText.setStyleRange(r);
               }
            }
         });
      }
   }

   public void updateTime(String time, String dayNight, String changeTime, String till, String dawnDusk)
   {
      if (timeText != null)
      {
         display.asyncExec(new Runnable()
         {
            StyleRange[] ranges = new StyleRange[2];

            @Override
            public void run()
            {
               ranges[0] = new StyleRange();
               ranges[1] = new StyleRange();
               ranges[0].underline = true;
               ranges[1].underline = true;

               // DAY DUSK
               // 10:54 19:00 (10:06)
               StringBuilder text = new StringBuilder();
               text.append("  ");
               ranges[0].start = 2;
               if (dayNight.length() == 3)
               {
                  ranges[0].start = 3;
                  text.append(" ");
               }
               ranges[0].length = dayNight.length();
               text.append(dayNight);
               while (text.length() < 15)
                  text.append(" ");
               text.append(dawnDusk);
               text.append("\n  ");
               text.append(time);
               text.append("    ");
               text.append(changeTime + " (" + till + ")");
               timeText.setText(text.toString());
               ranges[1].start = 15;
               ranges[1].length = dawnDusk.length();
               ranges[1].underline = true;
               timeText.setStyleRanges(ranges);
            }
         });
      }
   }

   private void createHpMpManaBars()
   {
      hpMpManaCanvas = new Canvas(shell, SWT.NO_REDRAW_RESIZE);
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = false;
      gridData.heightHint = 30;

      hpMpManaCanvas.setLayoutData(gridData);

      hpMpManaCanvas.addPaintListener(new PaintListener()
      {
         @Override
         public void paintControl(PaintEvent e)
         {
            int cHeight = hpMpManaCanvas.getClientArea().height;
            int cWidth = hpMpManaCanvas.getClientArea().width;
            e.gc.setBackground(BLACK);
            e.gc.fillRectangle(0, 0, cWidth, cHeight);
            e.gc.setForeground(GRAY);
            e.gc.setBackground(DARK_RED);
            e.gc.fillRectangle(0, 0, (int) (1.0 * hp_high / hp_max * cWidth), cHeight / 3 - 1);
            e.gc.setBackground(RED);
            e.gc.fillRectangle(0, 0, (int) (1.0 * hp_low / hp_max * cWidth), cHeight / 3 - 1);
            e.gc.drawRectangle(0, 0, (int) (1.0 * hp_low / hp_max * cWidth), cHeight / 3 - 1);
            e.gc.drawLine((int) (1.0 * hp_wimpy / hp_max * cWidth), 0,
                  (int) (1.0 * hp_wimpy / hp_max * cWidth), cHeight / 3 - 1);
            e.gc.drawRectangle(0, 0, (int) (1.0 * hp_high / hp_max * cWidth), cHeight / 3 - 1);
            if (hp_curr > 0)
            {
               e.gc.setBackground(BLACK);
               e.gc.fillRectangle((int) (1.0 * hp_curr / hp_max * cWidth - 1), 0, 2, cHeight / 3 - 1);
            }

            e.gc.setBackground(DARK_GREEN);
            e.gc.fillRectangle(0, cHeight / 3, (int) (1.0 * mv_high / mv_max * cWidth), cHeight / 3 - 1);
            e.gc.setBackground(GREEN);
            e.gc.fillRectangle(0, cHeight / 3, (int) (1.0 * mv_low / mv_max * cWidth), cHeight / 3 - 1);
            e.gc.drawRectangle(0, cHeight / 3, (int) (1.0 * mv_low / mv_max * cWidth), cHeight / 3 - 1);
            e.gc.drawRectangle(0, cHeight / 3, (int) (1.0 * mv_high / mv_max * cWidth), cHeight / 3 - 1);
            if (mv_curr > 0)
            {
               e.gc.setBackground(BLACK);
               e.gc.fillRectangle((int) (1.0 * mv_curr / mv_max * cWidth - 1), cHeight / 3, 2,
                     cHeight / 3 - 1);
            }

            e.gc.setBackground(DARK_BLUE);
            e.gc.fillRectangle(0, 2 * cHeight / 3, (int) (1.0 * mana_high / mana_max * cWidth),
                  cHeight / 3 - 1);
            e.gc.setBackground(BLUE);
            e.gc.fillRectangle(0, 2 * cHeight / 3, (int) (1.0 * mana_low / mana_max * cWidth),
                  cHeight / 3 - 1);
            e.gc.drawRectangle(0, 2 * cHeight / 3, (int) (1.0 * mana_low / mana_max * cWidth),
                  cHeight / 3 - 1);
            e.gc.drawRectangle(0, 2 * cHeight / 3, (int) (1.0 * mana_high / mana_max * cWidth),
                  cHeight / 3 - 1);
            if (mana_curr > 0)
            {
               e.gc.setBackground(BLACK);
               e.gc.fillRectangle((int) (1.0 * mana_curr / mana_max * cWidth - 1), 2 * cHeight / 3, 2,
                     cHeight / 3 - 1);
            }
         }
      });
   }

   private void createTimers()
   {

   }

   private void createRoomInfoBox()
   {
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = true;
      gridData.verticalAlignment = GridData.FILL;
      roomInfoText = new StyledText(shell, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
      roomInfoText.setLayoutData(gridData);
      Font mono = new Font(display, "Courier", 12, SWT.NONE);
      roomInfoText.setFont(mono);
      roomInfoText.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
      roomInfoText.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
   }

   private void createTimeBox()
   {
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = false;
      gridData.heightHint = 30;
      timeText = new StyledText(shell, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
      timeText.setLayoutData(gridData);
      Font mono = new Font(display, "Courier", 12, SWT.NONE);
      timeText.setFont(mono);
      timeText.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
      timeText.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
   }

   @Override
   public void start()
   {
      display = MudFrame.getInstance().getDisplay();
      try
      {
         int newFontSize = Integer.valueOf(Configger.getProperty("FONTSIZE", "12")).intValue();
         if (newFontSize != fontSize)
         {
            fontSize = newFontSize;
            Configger.setProperty("FONTSIZE", fontSize + "");
         }
      }
      catch (@SuppressWarnings("unused")
      NumberFormatException e)
      {
         log.add(Level.INFO, "Couldn't get font size from configger");
      }

      RED = display.getSystemColor(SWT.COLOR_RED);
      DARK_RED = display.getSystemColor(SWT.COLOR_DARK_RED);
      BLUE = display.getSystemColor(SWT.COLOR_BLUE);
      DARK_BLUE = display.getSystemColor(SWT.COLOR_DARK_BLUE);
      GREEN = display.getSystemColor(SWT.COLOR_GREEN);
      DARK_GREEN = display.getSystemColor(SWT.COLOR_DARK_GREEN);
      GRAY = display.getSystemColor(SWT.COLOR_GRAY);
      BLACK = display.getSystemColor(SWT.COLOR_BLACK);

      shell = new Shell(display);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      shell.setLayout(gridLayout);
      shell.setText("M.U.M.E. Info");

      // try to get the location and size from the Configger
      int xloc = 0;
      int yloc = 0;
      int height = 300;
      int width = 200;
      try
      {
         xloc = Integer.valueOf(Configger.getProperty("MUMEXLOC", "0")).intValue();
         yloc = Integer.valueOf(Configger.getProperty("MUMEYLOC", "0")).intValue();
         height = Integer.valueOf(Configger.getProperty("MUMEheight", "300")).intValue();
         width = Integer.valueOf(Configger.getProperty("MUMEwidth", "200")).intValue();
      }
      catch (NumberFormatException e)
      {
         log.add(Level.WARNING, "Couldn't get Configger values: ", e);
      }

      createHpMpManaBars();
      createTimers();
      createTimeBox();
      createRoomInfoBox();

      shell.setLocation(xloc, yloc);
      shell.setSize(width, height);

      // Save resize information to the config file
      shell.addListener(SWT.Resize, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            Configger.setProperty("MUMEwidth", shell.getSize().x + "");
            Configger.setProperty("MUMEheight", shell.getSize().y + "");
         }
      });

      // Save location information to the config file
      shell.addListener(SWT.Move, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            Configger.setProperty("MUMEXLOC", shell.getLocation().x + "");
            Configger.setProperty("MUMEYLOC", shell.getLocation().y + "");
         }
      });

      shell.open();
      isShown = true;

      shell.addListener(SWT.Close, new Listener()
      {
         @Override
         public void handleEvent(Event event)
         {
            System.out.println("Child Shell handling Close event, about to dispose this Shell");
            stop();
            shell.dispose();
            isShown = false;
         }
      });
      
      running = true;
   }

   @Override
   public void stop()
   {
      running = false;
   }
   
   @Override
   public boolean isRunning()
   {
      return running;
   }

   @Override
   public boolean isFormatter()
   {
      return false;
   }

   @Override
   public boolean isTriggerer()
   {
      return true;
   }

   @Override
   public boolean isCommander()
   {
      return false;
   }

   @Override
   public String format(String line, List<StyleRange> ranges)
   {
      throw new UnsupportedOperationException("format Not Implemented");
   }

   @Override
   public boolean command(String command)
   {
      throw new UnsupportedOperationException("command Not Implemented");
   }

}
