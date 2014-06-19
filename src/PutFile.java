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
	    if (args[1] != null) port = Integer.parseInt(args[1]);
	    String localDir = "c:/Users/pavel.naduda/Desktop/updates/r2d2m";
	    if (args[2] != null) localDir = args[2];
	    String remouteDir = "/home/powersys";
	    if (args[3] != null) remouteDir = args[3];
	    
	    long start = System.currentTimeMillis();
	    long b = start;
		try {
			session = jsch.getSession("root", server, port);
	        session.setConfig("StrictHostKeyChecking", "no");
	        session.setPassword("root");
	        session.connect();

	        System.out.println(session.getClientVersion() + " - " + (System.currentTimeMillis() - start)/1000 + " s");
//	        executeCommand(session, "reboot");
//	        System.out.println("reboot");
	        
	        executeCommand(session, "killall check_r2d2m.sh");
	        System.out.println("killall check_r2d2m.sh - " + (System.currentTimeMillis() - start)/1000 + " s");
	        executeCommand(session, "killall r2d2m");
	        System.out.println("killall r2d2m - " + (System.currentTimeMillis() - start)/1000 + " s");
	        
	        Channel channel = session.openChannel("sftp");
	        channel.connect();
	        sftpChannel = (ChannelSftp) channel;

	        sftpChannel.cd("/home/powersys/log/");
        	@SuppressWarnings("unchecked")
			Vector<ChannelSftp.LsEntry> list = sftpChannel.ls("/home/powersys/log/*"); 
        	for (ChannelSftp.LsEntry listEntry : list) {
        		executeCommand(session, "rm -rf /home/powersys/log/" + listEntry.getFilename());
        		System.out.println("rm -rf /home/powersys/log/" + listEntry.getFilename() + "   --> OK");
        	}
	        
        	System.out.println("coping ...");
	        sftpChannel.put(localDir, remouteDir);
	        System.out.println("coped c:/Users/pavel.naduda/Desktop/updates/r2d2m to /home/powersys");
	        System.out.println();
	        System.out.println("TOTAL TIME - " + (System.currentTimeMillis() - b)/1000 + " s");

	        executeCommand(session, "reboot");
	        System.out.println("reboot");
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
			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			
			((ChannelExec) channel).setCommand( script);
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
					break;
				}
				try {
					Thread.sleep(1000);
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