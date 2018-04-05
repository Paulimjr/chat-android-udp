package udp.chat.com.br.chatlocaludp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private EditText campoDigitar;
    private TextView mensagens;
    private Thread threadMensagens;
    private Thread threadEnviarMensagem;
    private DatagramSocket s;
    private InetAddress dest;
    DatagramPacket datagramPacket;
    private List<String> listaMensagens;
    String ipString;

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        campoDigitar = findViewById(R.id.digiteAqui);
        mensagens = findViewById(R.id.mensagens);
        try {
            int SDK_INT = android.os.Build.VERSION.SDK_INT;
            if (SDK_INT > 8)
            {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                        .permitAll().build();
                StrictMode.setThreadPolicy(policy);

                Intent it = getIntent();

                if (it != null) {
                    ipString = it.getStringExtra("serverIp");
                }
                System.out.println("IP CONNECTED: "+ipString);
                this.crateObjects(ipString);

            }
        } catch (UnknownHostException e) {
            System.out.println("UnknownHostException >>>>>> "+e.getMessage());
        } catch (SocketException e) {
            System.out.println("SocketException >>>>>> "+e.getMessage());
        }
    }

    private void crateObjects(String ipString) throws UnknownHostException, SocketException {
        this.dest = InetAddress.getByName(ipString);
        this.s = new DatagramSocket();
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Thread para pegar as mensagens chegar....
        threadMensagens = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        DatagramPacket resposta = new DatagramPacket(new byte[512], 512);
                        s.receive(resposta);
                        String printResposta = new String(resposta.getData());

                        //VERIFICANDO SE O CARA QUER SAIR...
                        if (resposta.getSocketAddress().equals(datagramPacket.getSocketAddress())) {
                            if (printResposta.trim().startsWith(Constants.LOGOUT)) {
                                System.exit(0);
                            }
                        }

                        String texto = mensagens.getText().toString();
                        texto = texto + printResposta + "\n";
                        final String finalTexto = texto;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mensagens.setText(finalTexto);
                            }
                        });


                    } catch (IOException ex) {
                        Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        threadMensagens.start(); //iniciando a thread...

        //Thread para enviar as mensagens
        threadEnviarMensagem = new Thread(new Runnable() {
            @Override
            public void run() {
                campoDigitar.setOnKeyListener(new View.OnKeyListener() {
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        // If the event is a key-down event on the "enter" button
                        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                                (keyCode == KeyEvent.KEYCODE_ENTER)) {
                            // Perform action on key press
                            String envio = campoDigitar.getText().toString();
                            if (envio.equalsIgnoreCase("")) {// Cliente disse que quer sair. HAHA
                                envio = Constants.LOGOUT;
                            }
                            //Envio de mensagem
                            try {
                                byte[] buffer = envio.getBytes();
                                datagramPacket = new DatagramPacket(buffer, buffer.length, dest, 4545);
                                s.send(datagramPacket);
                                campoDigitar.setText("");
                            } catch (IOException ex) {
                                System.out.println("IOExeption: "+ex.getMessage());
                            }

                            return true;
                        }
                        return false;
                    }
                });
            }
        });
        threadEnviarMensagem.start();
    }
}
