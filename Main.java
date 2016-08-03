/*
 * 访问远程数据库并显示相关数据
 * 进行数据的分页显示
 * 数据下拉刷新====>在第一个item可见并下拉一定距离时触发
 * 主要是进行各种状态的判断和对应的处理
 * 涉及到不同状态之间的切换，考虑使用状态模式进行重构
 * 将数据处理独立到后台的服务中去
 * 最终版本确定
 */
package com.example.showdata;

import java.util.ArrayList;
import java.util.List;

import com.example.showdata.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class Main extends Activity{
	
	//相关控件
	private ListView show;
	private Button pre,next,now;
	private Spinner type;
	private List<String> result=new ArrayList<String>();
	private Handler h=new Handler();
	final DatabaseHelp dh=DatabaseHelp.getDefault();
	private ArrayAdapter a;
	//数据相关
	private int nowPage=1;
	private int pageCount=0;
	int itemCount=0;
	//每页显示的最多数量，可自定义
	private final int showCount=15;
	//标记是否为最后一页等
	private boolean isLast=false;
	private boolean isFirstItem=true;
	private boolean isRecord=false;
	//加载进度条，保证其为单例
	ProgressDialog pd;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//加载过早，等待数据更新完成后再进行加载
		setContentView(R.layout.main);
		initView();
		loadContent(0,true);
	}
	
	//界面的初始化，这些都需要进行重用，不能去多次实例化，仅对变化的部分进行重新加载
	private void initView(){
		show=(ListView)findViewById(R.id.show);
		now=(Button)findViewById(R.id.now);
		pre=(Button)findViewById(R.id.pre);
		next=(Button)findViewById(R.id.next);
		type=(Spinner)findViewById(R.id.type);
		
		//下拉刷新的实现，主要是对滑动的判断
		show.setOnTouchListener(new View.OnTouchListener() {
			
			float yStart,yEnd,length,to;
			
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				// TODO Auto-generated method stub
				switch(event.getAction()){
				case(MotionEvent.ACTION_DOWN):
					//此处暂不处理
					//在处于第一个项目可见且还未记录位置时进行开始位置记录
					if(isFirstItem&&!isRecord){
						Log.v("down and first and not record","  ");
						isRecord=true;
						yStart=event.getY();
					}
					break;
				//抬起的时候根据移动的距离判断是否需要更新	
				case(MotionEvent.ACTION_UP):
					//得到滑动的距离
					yEnd=event.getY();
					length=yEnd-yStart;
					isRecord=false;
					Log.v("length up",length+"   ");
					if(length>30&&isFirstItem){
						//重新进行数据的加载，效率太低，数据量过多的时候会有很大的性能问题
						loadContent(0,true);
						//Toast.makeText(Main.this,"刷新中", Toast.LENGTH_LONG).show();
					}
					break;
				case(MotionEvent.ACTION_MOVE):
					//不断记录和更新位置，然后进行判断，如果位置到达了第一个数据条目，开始记录移动距离
					if(isFirstItem&&!isRecord){
						//如果到达第一项，那么开始记录这个位置
						yStart=event.getY();
						isRecord=true;
						Log.v("yStart",yStart+"  ");	
					}
					break;
				}
				return false;
			}
		});
		
		//滚动监听,主要是判断listview是否显示第一个item
		show.setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView arg0, int arg1) {
				// TODO Auto-generated method stub
			}
			
			@Override
			public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				isFirstItem=arg1==0?true:false;
				
			}
		});
		
		//下一页按钮的处理
		next.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.v("nowPage",nowPage+"   ");
				pre.setVisibility(View.VISIBLE);
				if(nowPage>=pageCount-1){
					next.setText("没了");
					isLast=true;
				}else{
					nowPage++;
					now.setText("第"+nowPage+"页");
					decideToShow();
				}
			}
		});
		
		//上一页按钮的处理
		pre.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(isLast){
					next.setText("下一页");
					isLast=false;
				}
				Log.v("nowPage",nowPage+"   ");
				// TODO Auto-generated method stub
				if(nowPage<2){
					pre.setVisibility(View.INVISIBLE);
				}else{
					nowPage--;
					now.setText("第"+nowPage+"页");
					if(nowPage==1){
						pre.setVisibility(View.INVISIBLE);
					}
					decideToShow();
				}
			}
		});
		
		//spinner的数据
		final String[] data=new String[]{"时间","金额","卡编号"};
		final ArrayAdapter a=new ArrayAdapter(this,android.R.layout.simple_list_item_1,data);
		type.setAdapter(a);
		
		type.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
			private boolean isInit=true;
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
					if(!isInit){
						doSelect(arg2);
						//a.notifyDataSetChanged();
						((TextView)arg1).setText(data[arg2]);
						Log.v("dd","dd");
					}
					isInit=false;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
	}
	
	//处理不同的排序方式
	private void doSelect(int type){
		loadContent(type,true);
		Log.v("type",type+"  ");
	}
	
	//确定需要显示的数据,其实在数据量不大的情况下可以事先分割好每一页要显示的数据，然后根据序号显示即可
	//需要优化
	private void decideToShow(){
		//马蛋，各种陷阱
		List<String> toShow=null;
		if(nowPage==pageCount-1){
			toShow=result.subList((nowPage-1)*showCount, (nowPage-1)*showCount+itemCount%showCount);
		}else{
			toShow=result.subList((nowPage-1)*showCount, (nowPage-1)*showCount+showCount);
		}
		Log.v("toShow",toShow.size()+"  ");
		//now.setText(nowPage);
		//nowPage++;
		a=new ArrayAdapter(Main.this,android.R.layout.simple_list_item_1,toShow);
		show.setAdapter(a);
	}
	
	//页面加载，考虑到数据获取的延迟问题
	final Runnable content=new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			pd.dismiss();
			if(!result.isEmpty()){
				//initView();
				itemCount=result.size();
				//数据填充，感觉重复了
				pageCount=itemCount%showCount!=0?itemCount/showCount+1:itemCount/showCount;
				Log.v("pageCount",pageCount+"   ");
				decideToShow();
			}else{
				TextView tv=new TextView(Main.this);
				tv.setText("无数据");
				setContentView(tv);
			}
			init();
		}
		
	};
	
	//初始化
	private void init(){
		now.setText("第一页");
		nowPage=1;
		pre.setVisibility(View.INVISIBLE);
		isLast=false;
		isFirstItem=true;
		isRecord=false;
	}
	
	//dialog的显示
	final Runnable dialog=new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			pd=ProgressDialog.show(Main.this,"","数据加载中", true);
		}
		
	};
	
	//数据的显示和刷新，需要开启一个新的线程进行数据获取
	//重新洗牌，很简单的
	//用了太多的runnable，就是因为他娘的安卓不能在非ui线程进行ui操作
	//有点蛋疼啊
	private void loadContent(final int type,final boolean desc){
		Log.v("loadContent",type+"  "+desc);
		new Thread(){
			
			@Override
			public void run(){
				//加载进度条对话框
				h.post(dialog);
				try {
					Thread.currentThread().sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//数据库查询，堵塞
				result=dh.query(type,desc);
				//这样写就好多了
				h.post(content);
			}
		}.start();
	}
	
}
