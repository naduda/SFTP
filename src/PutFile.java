import java.io.InputStream;
import java.util.Vector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class PutFile {
	public static void main(String[] args) {
		JSch jsch = new JSch();
	    Session session = null;
	    ChannelSftp sftpChannel = null;
	    
	    String server = args[0];
	    int port = 22;
	    if (args.length > 1) port = Integer.parseInt(args[1]);
	    String localDir = "c:/Users/pavel.naduda/Desktop/updates/r2d2m";
	    if (args.length > 2) localDir = args[2];
	    String remouteDir = "/home/powersys";
	    if (args.length > 3) remouteDir = args[3];
	    
	    long start = System.currentTimeMillis();
	    long b = start;
		try {
			session = jsch.getSession("root", server, port);
	        session.setConfig("StrictHostKeyChecking", "no");
	        session.setPassword("root");
	        session.connect();

	        System.out.println(session.getClientVersion() + " - " + (System.currentTimeMillis() - start)/1000 + " s");
	        
	        executeCommand(session, "killall check_r2d2m.sh");
	        executeCommand(session, "killall r2d2m");
	        
	        Channel channel = session.openChannel("sftp");
	        channel.connect();
	        sftpChannel = (ChannelSftp) channel;

	        sftpChannel.cd("/home/powersys/log/");
        	@SuppressWarnings("unchecked")
			Vector<ChannelSftp.LsEntry> list = sftpChannel.ls("/home/powersys/log/*"); 
        	for (ChannelSftp.LsEntry listEntry : list) {
        		executeCommand(session, "rm -rf /home/powersys/log/" + listEntry.getFilename());
        	}
	        
        	System.out.println("coping ...");
	        sftpChannel.put(localDir, remouteDir);
	        System.out.println("coped c:/Users/pavel.naduda/Desktop/updates/r2d2m to /home/powersys");
	        System.out.println();
	        long totalTime = (System.currentTimeMillis() - b)/1000;
	        System.out.print("TOTAL TIME - " + totalTime + " s");
	        if (totalTime > 60) System.out.print("   -->   " + (int)totalTime/60 + " min");
	        System.out.println();

	        executeCommand(session, "reboot");
	    } catch (JSchException | SftpException e) {
	    	System.err.println("Error in main   !!!");
	        e.printStackTrace(); 
	    } finally {
	    	sftpChannel.exit();
	        session.disconnect();
	    }
	}
	
	public static void executeCommand(Session session, String script) {
		try {
			System.out.print(script + "   -->   ");
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			
			((ChannelExec) channel).setCommand(script);
			InputStream in = channel.getInputStream();
			((ChannelExec) channel).setErrStream(System.err);

			channel.connect();
			
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					System.out.print(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					System.out.println("Successfully!");
					break;
				}
				try {
					Thread.sleep(500);
				} catch (Exception ee) {
					System.err.println(ee);
				}
			}
			channel.disconnect();
		} catch (Exception e) {
			System.err.println("Error in executeCommand()   !!!");
			e.printStackTrace();
		}
	}
}
