package com.umarabdul.networking.jmailer;

import com.umarabdul.wrapper.networking.SocketWrapper;
import java.util.*;
import java.net.Socket;
import java.io.*;

/**
* JMailer is a simple library for sending emails using an SMTP server.
*
* @author Umar Abdul
* @version 1.0
* @since 2020
*/

public class JMailer{

  private String host;
  private String domain;
  private ArrayList<String> recipients;
  private String sender = null;
  private String subject = null;

  /**
  * JMailer's constructor.
  * @param host Host running SMTP server.
  * @param domain Domain name to use in EHLO command.
  */
  public JMailer(String host, String domain){

    this.host = host;
    this.domain = domain;
    recipients = new ArrayList<String>();
  }

  /**
  * Add an email address to list of recipients.
  * @param address Email address of recipient.
  */
  public void addRecipient(String address){

    if (!(recipients.contains(address)))
      recipients.add(address);
  }

  /**
  * Remove an email address from list of recipients.
  * @param address Email address to remove.
  */
  public void removeRecipient(String address){
    recipients.remove(address);
  }

  /**
  * Obtain the ArrayList of recipients.
  * @return An ArrayList of email recipients.
  */
  public ArrayList<String> getRecipients(){
    return recipients;
  }

  /**
  * Set email address of sender.
  * @param address Email address of sender.
  */
  public void setSender(String address){
    sender = address;
  }

  /**
  * Set subject of email to send.
  * @param subject Email subject.
  */
  public void setSubject(String subject){
    this.subject = subject;
  }

  /**
  * Send the email, provided all requirements are satisfied.
  * @param body Body of email to send.
  * @return {@code true} on success.
  */
  public boolean send(String body){

    try{
      if (sender == null || subject == null || recipients.size() == 0)
        return false;
      String rsp = null;
      // Create and wrap socket.
      SocketWrapper conn = new SocketWrapper(new Socket(host, 25));
      conn.read(1024); // banner.
      conn.write(String.format("EHLO %s\r\n", domain));
      rsp = conn.read(1024);
      if (!(rsp.startsWith("250")))
        return false;
      // Add mail sender and recipients addresses.
      conn.write(String.format("mail from: %s\r\n", sender));
      rsp = conn.read(1024);
      if (!(rsp.startsWith("250")))
        return false;
      for (String addr : recipients){
        conn.write(String.format("rcpt to: %s\r\n", addr));
        rsp = conn.read(1024);
        if (!(rsp.startsWith("250")))
          return false;
      }
      conn.write("data\r\n");
      rsp = conn.read(1024);
      if (!(rsp.startsWith("354")))
        return false;
      String data = String.format("Subject: %s\r\n\r\n", subject);
      data += body;
      data += "\r\n.\r\n";
      conn.write(data);
      rsp = conn.read(1024);
      if (!(rsp.startsWith("250")))
        return false;
      conn.write("quit\r\n");
      conn.getSocket().close();
      return true;
    }catch(Exception e){
      System.out.println(String.format("[-] JMailer: %s : %s", e.getClass().getName(), e.getMessage()));
      return false;
    }
  }

  /**
  * Used by JMailer.main() for prompting user input.
  * @param p Prompt to display.
  */
  public static void prompt(String p){
    System.out.print("[?] " +p+ " > ");
  }

  /**
  * Runs JMailer interactively.
  * @param args Command-line args.
  * @throws Exception on error.
  */
  public static void main(String[] args) throws Exception{

    Scanner sc = new Scanner(System.in);
    prompt("Server host");
    String host = sc.nextLine();
    prompt("EHLO domain");
    String domain = sc.nextLine();
    prompt("From");
    String sender = sc.nextLine();
    prompt("To (separator = ':')");
    String[] rcpts = sc.nextLine().trim().split(":");
    prompt("Subject");
    String subject = sc.nextLine();
    prompt("Email body file");
    File file = new File(sc.nextLine());
    if (!(file.isFile())){
      System.out.println("[-] Invalid file!");
      return;
    }
    BufferedReader reader = new BufferedReader(new FileReader(file));
    char[] body = new char[(int)(file.length())];
    reader.read(body, 0, (int)(file.length()));
    JMailer mailer = new JMailer(host, domain);
    mailer.setSender(sender);
    mailer.setSubject(subject);
    for (String addr : rcpts)
      mailer.addRecipient(addr.trim());
    System.out.println("[+] Sending mail...");
    if (mailer.send(new String(body)))
      System.out.println("[+] Mail sent successfully!");
    else
      System.out.println("[-] Error sending mail!");
  }

}

