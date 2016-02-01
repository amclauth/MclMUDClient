package com.mcltech.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class MudFrame
{
   Shell               shell;
   private Display             display;
   private static final Logger log = Logger.getLogger(MudFrame.class.getName());
   StyledText outputText;
   Text inputText;
   Controller controller;
   
   public MudFrame()
   {
      
   }
   
   public void init()
   {
      display = new Display();
      shell = new Shell(display);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      shell.setLayout(gridLayout);
      shell.setText("MclMUD Client");

      createMenu();
      createOutputScreen();
      createInputScreen();

      controller = new Controller(outputText);
      controller.init(display);
      
      int xloc = 0;
      int yloc = 0;
      int height = 600;
      int width = 800;
      try
      {
         xloc = Integer.valueOf(Configger.getProperty("XLOC", "0")).intValue();
         yloc = Integer.valueOf(Configger.getProperty("YLOC", "0")).intValue();
         height = Integer.valueOf(Configger.getProperty("height", "600")).intValue();
         width = Integer.valueOf(Configger.getProperty("width", "800")).intValue();
      }
      catch (NumberFormatException e)
      {
         log.warning("Couldn't get Configger values: " + e.getMessage());
      }
      
      shell.setLocation(xloc,yloc);
      shell.setSize(width,height);
      
      shell.addListener (SWT.Resize,  new Listener () {
         @Override
         public void handleEvent (Event e) {
            Configger.setProperty("width", shell.getSize().x + "");
            Configger.setProperty("height", shell.getSize().y + "");
         }
      });
      
      shell.addListener (SWT.Move,  new Listener () {
         @Override
         public void handleEvent (Event e) {
            Configger.setProperty("XLOC", shell.getLocation().x + "");
            Configger.setProperty("YLOC", shell.getLocation().y + "");
         }
      });
      
      shell.open();
      
      appendToText("Welcome to the MclMUD client\n\n");
      appendToText("To join a MUD, use the \"Connect\" button in the menu above or add a new connection\n");
      appendToText("by entering the name:address:port below. For example, to create a MUME connection, enter:\nMUME:mume.org:4242\n");
      
      while (!shell.isDisposed())
      {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }
   
   private void createOutputScreen()
   {
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = true;
      gridData.verticalAlignment = GridData.FILL;
      outputText = new StyledText(shell, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
      outputText.setLayoutData(gridData);
      Font mono = new Font(display, "Courier", 12, SWT.NONE);
      outputText.setFont(mono);
      outputText.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
      outputText.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
      outputText.addListener(SWT.Modify, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            outputText.setTopIndex(outputText.getLineCount() - 1);
         }
      });
      outputText.addListener(SWT.MouseUp, new Listener()
      {
         @Override
         public void handleEvent(Event e)
         {
            inputText.setFocus();
         }
      });

   }
   
   private void createInputScreen()
   {
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.verticalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = false;
      gridData.heightHint = 50;
      inputText = new Text(shell, SWT.SINGLE | SWT.BORDER | SWT.WRAP);
      inputText.setLayoutData(gridData);
      inputText.addListener(SWT.Traverse, new Listener() {
         @Override
         public void handleEvent(Event e) {
            if (e.detail == SWT.TRAVERSE_RETURN)
            {
               if (!controller.isConnected())
               {
                  addConnection(inputText.getText());
               }
               controller.write(inputText.getText() + "\n");
               inputText.setText("");
            }
         }
       });
   }
   
   private void createMenu()
   {
      Menu menuBar = new Menu(shell, SWT.BAR);
      
      MenuItem connectionMenuItem = new MenuItem(menuBar, SWT.CASCADE);
      connectionMenuItem.setText("&Connect");
      
      Menu connectionMenu = new Menu(shell, SWT.DROP_DOWN);
      connectionMenuItem.setMenu(connectionMenu);
      
      List<String> muds = new ArrayList<>();
      for (String mud : Configger.getProperty("MUDS", "").split(":"))
      {
         if (!mud.equals(""))
            muds.add(mud);
      }
      Collections.sort(muds);
      
      for (String mud : muds)
      {
         String[] details = Configger.getProperty(mud, "").split(":");
         if (details.length != 2)
         {
            log.warning("Got bad data for mud: " + mud);
            continue;
         }
         MenuItem mumeItem = new MenuItem(connectionMenu, SWT.PUSH);
         mumeItem.setText("&" + mud);
         mumeItem.addSelectionListener(new SelectionAdapter()
         {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
               controller.connect(details[0], Integer.valueOf(details[1]).intValue());
            }
         });
      }
      
      MenuItem disconnectMenuItem = new MenuItem(menuBar, SWT.CASCADE);
      disconnectMenuItem.setText("&Disconnect");
      disconnectMenuItem.addSelectionListener(new SelectionAdapter()
      {
         @Override
         public void widgetSelected(SelectionEvent e)
         {
            controller.disconnect();
         }
      });
      
      shell.setMenuBar(menuBar);
   }
   
   private void appendToText(String line)
   {
      Display.getDefault().asyncExec(new Runnable()
      {
         @Override
         public void run()
         {
            outputText.append(line);
         }
      });
   }
   
   void addConnection(String line)
   {
      int idx1 = line.indexOf(':');
      int idx2 = line.indexOf(':', idx1+1);
      if (idx1 > 0 && idx2 > 0)
      {
         String name = line.substring(0, idx1);
         String con = line.substring(idx1+1,idx2);
         String pString = line.substring(idx2+1);
         int port = 0;
         try {
            port = Integer.valueOf(pString).intValue();
         }
         catch (NumberFormatException e)
         {
            appendToText("\nPort wasn't a number. Got name{" + name + "} address{" + con + "} port{" + pString + "}\n");
            log.warning("Couldn't parse number: " + e.getMessage());
            return;
         }
         
         appendToText("\nAdding connection name{" + name + "} address{" + con + "} port{" + pString + "}\n");
         
         System.out.println(name + "," + con + "," + port);
         String muds = Configger.getProperty("MUDS", "");
         if (Configger.getProperty(name, "").equals(""))
         {
            // new one
            List<String> arr = new ArrayList<>();
            for (String s : muds.split(":"))
            {
               if (!s.equals(""))
                  arr.add(s);
            }
            arr.add(name);
            Configger.setProperty("MUDS", String.join(":",arr));
            Configger.setProperty(name, con + ":" + pString);
         }
         else
         {
            // replacement
            Configger.setProperty(name, con + ":" + pString);
         }
         
         createMenu();
         shell.update();
      } 
      else
      {
         appendToText("\nIf entering a connection, need that in name:address:port style.\n");
         return;
      }
   }
}
