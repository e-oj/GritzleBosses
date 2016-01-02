import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;

/**
 * This Program queries my Gritzle aka Cyclops api
 * for the top 50 posts, then it displays the username
 * and Profile image of all the creators of those posts.
 *
 * @author Emmanuel Olaojo
 * @since 1/1/16
 */
public class GritzleBosses extends JFrame{
    private static final String GRITZLE = "http://www.webct.net:8080/api";
//    private static final String GRITZLE = "http://localhost:8080/api";
    private static final String TOP_POSTS = GRITZLE + "/posts/top50";
    private static final String MEDIA = GRITZLE + "/media/";
    private final HashSet<String> users = new HashSet<>();
    private final JLabel jlLoading = new JLabel();
    JButton jbRefresh = new JButton("Refresh");
    JPanel jpContainer = new JPanel(new GridLayout(0, 3, 0, 10));
    /**
     * Default constructor builds the GUI
     */
    public GritzleBosses(){
        this.buildGui();
        this.setupViewPane();
    }

    /**
     * This Method does all the necessary setup to have the GUI
     * Running.
     */
    public void buildGui(){
        //displays a friendly view while GUI loads
        this.loading();

        this.setTitle("Gritzle - Top Users");
        this.pack();
        this.setResizable(false);
        this.setBackground(Color.LIGHT_GRAY);
        this.setVisible(true);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
    }

    /**
     * Sets Up a view with information from the server
     * and adds a refresh button
     */
    public void setupViewPane(){
        /* Updating via Thread prevents the UI from blocking
         * while the application waits for a response from the
         * server
         */
        new Thread(()->{
            jpContainer.setBackground(Color.LIGHT_GRAY);
            this.addResultsToPanel(jpContainer);

            JPanel jpRefresh = new JPanel();
            jbRefresh.addActionListener(e -> this.refresh());
            jpRefresh.add(jbRefresh);

            this.remove(jlLoading);
            this.add(jpContainer);
            this.add(jpRefresh, BorderLayout.SOUTH);

            this.pack();
            this.setLocationRelativeTo(null);
        }).start();
    }

    /**
     * Displays a user friendly message and a Pre-loader
     * While the user waits for the GUI to load
     */
    public void loading(){
        try {
            ImageIcon loadGif = new ImageIcon(this.getClass().getResource("loading.GIF"));
            jlLoading.setIcon(loadGif);
            jlLoading.setText("    Awaiting response from server    ");
            this.add(jlLoading);
            this.validate();
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    /**
     * Sends a request to the api for the top 50
     * posts.
     *
     * @return a JSONObject holding the response
     */
    private JSONObject getTop50(){
        StringBuilder sb = new StringBuilder();

        try {
            URL url = new URL(TOP_POSTS);
            URLConnection connection = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            br.lines().forEach(sb::append);

        } catch (UnknownHostException uhe){
            String message = "An internet connection is necessary for this\n" +
                    "program to work. Please connect to the internet \nand try again";

            JOptionPane.showMessageDialog(this, message);
            System.exit(0);
        }catch(Exception e){
            e.printStackTrace();
        }

        return new JSONObject(sb.toString());
    }

    /**
     * Sends a request to the api for media with the
     * given imageId
     *
     * @param imageId the id to be requested
     *
     * @return the requested Image
     */
    private Image getImage(String imageId){
        Image image = null;

        try{
            URL url = new URL(MEDIA + imageId);
            image = ImageIO.read(url);
        } catch (Exception e){
            System.out.println(e.getMessage());
        }

        return image;
    }

    /**
     * This method goes through all the returned posta
     * and gets the profile image and username of each post's
     * creator then it adds this information to the given panel.
     *
     * TODO: set size of grid dynamically to fit content
     *
     * @param panel The JPanel to work with
     */
    private void addResultsToPanel(JPanel panel){
        final int IMG_WIDTH = 200;
        final int IMG_HEIGHT = 200;
        JSONObject response = this.getTop50();
        JSONArray result = response.getJSONArray("result");

        for(int i=0; i<result.length(); i++){
            JSONObject author = result.getJSONObject(i).getJSONObject("author");
            String username = (String)author.get("username");
            String mediaID = (String)author.getJSONObject("profileMedia").get("media");
            Image profilePic = this.getImage(mediaID).getScaledInstance(IMG_WIDTH, IMG_HEIGHT, Image.SCALE_FAST);

            if(profilePic != null && !users.contains(username)){
                users.add(username);

                JPanel jpContainer = new JPanel(new BorderLayout());
                    JLabel jlUsername = new JLabel(username);
                    JLabel jlProfilePic = new JLabel(new ImageIcon(profilePic));
                jpContainer.add(jlUsername, BorderLayout.CENTER);
                jpContainer.add(jlProfilePic, BorderLayout.SOUTH);
                jpContainer.setBackground(Color.LIGHT_GRAY);

                panel.add(jpContainer);
            }
        }
        System.out.println(users);
    }

    /**
     * Displays the loading panel and starts a thread
     * that updates the results
     */
    public void refresh(){
        this.remove(this.jpContainer);
        this.add(this.jlLoading);
        this.revalidate();
        this.repaint();
        this.pack();
        this.setLocationRelativeTo(null);

        /* Updating via Thread prevents the UI from blocking
         * while the application waits for a response from the
         * server
         */
        Thread updateThread = new Thread(() -> {
            this.jbRefresh.setEnabled(false);

            /* 200ms sleep to ensure that the user always sees
             * the loading gif and message
             */
            try{
                Thread.sleep(200);
            } catch (InterruptedException ie){
                System.out.println(ie.getMessage());
            }

            this.addResultsToPanel(this.jpContainer);
            this.remove(this.jlLoading);
            this.add(this.jpContainer);
            this.jbRefresh.setEnabled(true);
            this.pack();
            this.revalidate();
            this.repaint();
            this.setLocationRelativeTo(null);
        });
        updateThread.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GritzleBosses::new);
    }
}
