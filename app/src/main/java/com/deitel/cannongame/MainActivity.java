// MainActivity.java
// MainActivity exibe o elemento CannonGameFragment
package com.deitel.cannongame;

import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    // chamado quando o aplicativo é ativado pela primeira vez
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // chama o método onCreate de Super
        setContentView(R.layout.activity_main); // infla o layout
    }
} // fim da classe MainActivity
