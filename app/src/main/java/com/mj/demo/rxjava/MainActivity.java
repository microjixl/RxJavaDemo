package com.mj.demo.rxjava;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    AtomicInteger id = new AtomicInteger();
    @Bind(R.id.info)
    TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        buildThrottleFirst();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_merge:
                merge();
                break;
            case R.id.action_block:
                block();
                break;
            case R.id.action_delay:
                delay();
                break;
            case R.id.action_interval:
                interval();
                break;
            case R.id.action_timeout:
                timeout();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    Disposable disposable = null;
    private void disposableTest() {
        /*disposable = */
        makeJob()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(4, TimeUnit.SECONDS)
                .doAfterNext(o -> printLog("doAfterNext:" + o.toString()))
                .doOnNext(o -> printLog("doOnNext:" + o.toString()))
                .doOnSubscribe(o -> printLog("doOnSubscribe:" + o.toString()))
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Object o) {
                        printLog("onNext:" + o.toString());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        printLog("onError:" + e.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
        printLog("isDisposed:" + disposable.isDisposed());
    }

    private void intervalTest() {
        printLog("begin>>>");
        Observable.interval(1, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::printLog);
    }

    private void merge() {
        Observable.mergeArray(makeJob(), makeAsyncJob(), makeJob())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::printLog, throwable -> {
                }, () -> printLog("complete"));
    }

    private void block() {
        String value = makeAsyncJob()
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .blockingFirst()
                .toString();
        printLog(value);
    }

    private void delay() {
        makeJob()
                .delay(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::printLog);
    }

    private void interval() {
        Observable.interval(5, TimeUnit.SECONDS)
                .flatMap(aLong -> makeJob())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::printLog);
    }

    private void timeout() {
        makeJob()
                .timeout(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {
                }, throwable -> printLog(throwable.toString()));
    }

    private void buildThrottleFirst() {
        Observable.create(e -> emitter = e)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::printLog);
    }

    private void buildDebounce() {
        Observable.create(e -> debounceEmitter = e)
                .debounce(4000, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::printLog);
    }

    private ObservableEmitter emitter;
    private ObservableEmitter debounceEmitter;

    private void printLog(Object o) {
        Log.e("TAG", "thread:[" + Thread.currentThread().getName() + "] ==" + o.toString());
        info.setText(o.toString());
    }

    private Observable<Object> makeJob() {
        return Observable.create(e -> {
            Log.e("TAG", "thread:" + Thread.currentThread().getName());
            e.onNext("job" + id.incrementAndGet());
            e.onComplete();
        });
    }

    private Observable<Object> makeAsyncJob() {
        return Observable.create(e -> {
            int makeId = this.id.incrementAndGet();
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                e.onNext("async job" + makeId);
                e.onComplete();
            }).start();
        });
    }
}
