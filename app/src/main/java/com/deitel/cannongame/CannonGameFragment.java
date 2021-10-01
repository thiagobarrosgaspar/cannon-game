// CannonGameFragment.java
// CannonGameFragment cria e gerencia um componente CannonView
package com.deitel.cannongame;

import android.app.Fragment;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CannonGameFragment extends Fragment
{
    private CannonView cannonView;    // view personalizada para mostrar o jogo

    // chamado quando a view do fragmento precisa ser criada
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        View view =
                inflater.inflate(R.layout.fragment_game, container, false);

        // obtém o componente CannonView
        cannonView = (CannonView) view.findViewById(R.id.cannonView);
        return view;
    }

    // configura o controle de volume quando a atividade é criada
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        // permite que as teclas de volume ajustem o volume do jogo
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }

        // quando MainActivity é pausada, CannonGameFragment termina o jogo
        @Override
    public void onPause()
    {
        super.onPause();
        cannonView.stopGame();    // termina o jogo
    }

    // quando MainActivity é pausada, CannonGameFragment libera os recursos
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        cannonView.releaseResources();
    }
} // fim da classe CannonGameFragment