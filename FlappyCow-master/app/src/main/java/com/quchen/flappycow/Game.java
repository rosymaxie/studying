
package com.quchen.flappycow;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;

public class Game extends BaseGameActivity{
    public static final String coin_save = "coin_save";
    
    public static final String coin_key = "coin_key";
    
    public static SoundPool soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
    
    private static final int GAMES_PER_AD = 3;
    private static int gameOverCounter = 1;
    private InterstitialAd interstitial;
    

    public static MediaPlayer musicPlayer = null;
    
    public boolean musicShouldPlay = false;
    
    private static final long DOUBLE_BACK_TIME = 1000;
    
    private long backPressed;
    
    public MyHandler handler;
    
    AccomplishmentBox accomplishmentBox;
    
    GameView view;
    
    /** The amount of collected coins */
    int coins;
    
    /** This will increase the revive price */
    public int numberOfRevive = 1;
    
    /** The dialog displayed when the game is over*/
    GameOverDialog gameOverDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accomplishmentBox = new AccomplishmentBox();
        view = new GameView(this);
        gameOverDialog = new GameOverDialog(this);
        handler = new MyHandler(this);
        setContentView(view);
        initMusicPlayer();
        loadCoins();
        if(gameOverCounter % GAMES_PER_AD == 0) {
            setupAd();
        }
    }

    public void initMusicPlayer(){
        if(musicPlayer == null){
            // to avoid unnecessary reinitialisation
            musicPlayer = MediaPlayer.create(this, R.raw.nyan_cat_theme);
            musicPlayer.setLooping(true);
            musicPlayer.setVolume(MainActivity.volume, MainActivity.volume);
        }
        musicPlayer.seekTo(0);    // Reset song to position 0
    }
    
    private void loadCoins(){
        SharedPreferences saves = this.getSharedPreferences(coin_save, 0);
        this.coins = saves.getInt(coin_key, 0);
    }

    @Override
    protected void onPause() {
        view.pause();
        if(musicPlayer.isPlaying()){
            musicPlayer.pause();
        }
        super.onPause();
    }


    @Override
    protected void onResume() {
        view.drawOnce();
        if(musicShouldPlay){
            musicPlayer.start();
        }
        if(GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS){
            Toast.makeText(this, "Please check your Google Services", Toast.LENGTH_LONG).show();
        }
        super.onResume();
    }
    @Override
    public void onBackPressed() {
        if(System.currentTimeMillis() - backPressed < DOUBLE_BACK_TIME){
            super.onBackPressed();
        }else{
            backPressed = System.currentTimeMillis();
            Toast.makeText(this, getResources().getString(R.string.on_back_press), Toast.LENGTH_LONG).show();
        }
    }


    public void gameOver(){
        if(gameOverCounter % GAMES_PER_AD == 0) {
            handler.sendMessage(Message.obtain(handler, MyHandler.SHOW_AD));
        } else {
            handler.sendMessage(Message.obtain(handler, MyHandler.GAME_OVER_DIALOG));
        }
        
    }
    
    public void increaseCoin(){
        this.coins++;
        if(coins >= 50 && !accomplishmentBox.achievement_50_coins){
            accomplishmentBox.achievement_50_coins = true;
            if(getApiClient().isConnected()){
                Games.Achievements.unlock(getApiClient(), getResources().getString(R.string.achievement_50_coins));
            }else{
                handler.sendMessage(Message.obtain(handler,1,R.string.toast_achievement_50_coins, MyHandler.SHOW_TOAST));
            }
        }
    }

    public void increasePoints(){
        accomplishmentBox.points++;
        
        this.view.getPlayer().upgradeBitmap(accomplishmentBox.points);
        
        if(accomplishmentBox.points >= AccomplishmentBox.BRONZE_POINTS){
            if(!accomplishmentBox.achievement_bronze){
                accomplishmentBox.achievement_bronze = true;
                if(getApiClient().isConnected()){
                    Games.Achievements.unlock(getApiClient(), getResources().getString(R.string.achievement_bronze));
                }else{
                    handler.sendMessage(Message.obtain(handler, MyHandler.SHOW_TOAST, R.string.toast_achievement_bronze, MyHandler.SHOW_TOAST));
                }
            }
            
            if(accomplishmentBox.points >= AccomplishmentBox.SILVER_POINTS){
                if(!accomplishmentBox.achievement_silver){
                    accomplishmentBox.achievement_silver = true;
                    if(getApiClient().isConnected()){
                        Games.Achievements.unlock(getApiClient(), getResources().getString(R.string.achievement_silver));
                    }else{
                        handler.sendMessage(Message.obtain(handler, MyHandler.SHOW_TOAST, R.string.toast_achievement_silver, MyHandler.SHOW_TOAST));
                    }
                }
                
                if(accomplishmentBox.points >= AccomplishmentBox.GOLD_POINTS){
                    if(!accomplishmentBox.achievement_gold){
                        accomplishmentBox.achievement_gold = true;
                        if(getApiClient().isConnected()){
                            Games.Achievements.unlock(getApiClient(), getResources().getString(R.string.achievement_gold));
                        }else{
                            handler.sendMessage(Message.obtain(handler, MyHandler.SHOW_TOAST, R.string.toast_achievement_gold, MyHandler.SHOW_TOAST));
                        }
                    }
                }
            }
        }
    }
    
    public GoogleApiClient getApiClient(){
        return mHelper.getApiClient();
    }
    
    static class MyHandler extends Handler{
        public static final int GAME_OVER_DIALOG = 0;
        public static final int SHOW_TOAST = 1;
        public static final int SHOW_AD = 2;
        
        private Game game;
        
        public MyHandler(Game game){
            this.game = game;
        }
        
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case GAME_OVER_DIALOG:
                    showGameOverDialog();
                    break;
                case SHOW_TOAST:
                    Toast.makeText(game, msg.arg1, Toast.LENGTH_SHORT).show();
                    break;
                case SHOW_AD:
                    showAd();
                    break;
            }
        }
        
        private void showAd() {
            if(game.interstitial == null) {
                showGameOverDialog();
            } else {
                if(game.interstitial.isLoaded()) {
                    game.interstitial.show();
                } else {
                    showGameOverDialog();
                }
            }
        }
        
        private void showGameOverDialog() {
            ++Game.gameOverCounter;
            game.gameOverDialog.init();
            game.gameOverDialog.show();
        }
    }
    


    @Override
    public void onSignInFailed() {}

    @Override
    public void onSignInSucceeded() {}
    
    private void setupAd() {
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId(getResources().getString(R.string.ad_unit_id));
        AdRequest adRequest = new AdRequest.Builder().build();
        interstitial.loadAd(adRequest);
        interstitial.setAdListener(new MyAdListener());
    }
    
    
    private class MyAdListener extends AdListener{
        public void onAdClosed () {
            handler.sendMessage(Message.obtain(handler, MyHandler.GAME_OVER_DIALOG));
        }
    }
}
