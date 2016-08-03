/*
 * ����Զ�����ݿⲢ��ʾ�������
 * �������ݵķ�ҳ��ʾ
 * ��������ˢ��====>�ڵ�һ��item�ɼ�������һ������ʱ����
 * ��Ҫ�ǽ��и���״̬���жϺͶ�Ӧ�Ĵ���
 * �漰����ͬ״̬֮����л�������ʹ��״̬ģʽ�����ع�
 * �����ݴ����������̨�ķ�����ȥ
 * ���հ汾ȷ��
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
	
	//��ؿؼ�
	private ListView show;
	private Button pre,next,now;
	private Spinner type;
	private List<String> result=new ArrayList<String>();
	private Handler h=new Handler();
	final DatabaseHelp dh=DatabaseHelp.getDefault();
	private ArrayAdapter a;
	//�������
	private int nowPage=1;
	private int pageCount=0;
	int itemCount=0;
	//ÿҳ��ʾ��������������Զ���
	private final int showCount=15;
	//����Ƿ�Ϊ���һҳ��
	private boolean isLast=false;
	private boolean isFirstItem=true;
	private boolean isRecord=false;
	//���ؽ���������֤��Ϊ����
	ProgressDialog pd;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//���ع��磬�ȴ����ݸ�����ɺ��ٽ��м���
		setContentView(R.layout.main);
		initView();
		loadContent(0,true);
	}
	
	//����ĳ�ʼ������Щ����Ҫ�������ã�����ȥ���ʵ���������Ա仯�Ĳ��ֽ������¼���
	private void initView(){
		show=(ListView)findViewById(R.id.show);
		now=(Button)findViewById(R.id.now);
		pre=(Button)findViewById(R.id.pre);
		next=(Button)findViewById(R.id.next);
		type=(Spinner)findViewById(R.id.type);
		
		//����ˢ�µ�ʵ�֣���Ҫ�ǶԻ������ж�
		show.setOnTouchListener(new View.OnTouchListener() {
			
			float yStart,yEnd,length,to;
			
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				// TODO Auto-generated method stub
				switch(event.getAction()){
				case(MotionEvent.ACTION_DOWN):
					//�˴��ݲ�����
					//�ڴ��ڵ�һ����Ŀ�ɼ��һ�δ��¼λ��ʱ���п�ʼλ�ü�¼
					if(isFirstItem&&!isRecord){
						Log.v("down and first and not record","  ");
						isRecord=true;
						yStart=event.getY();
					}
					break;
				//̧���ʱ������ƶ��ľ����ж��Ƿ���Ҫ����	
				case(MotionEvent.ACTION_UP):
					//�õ������ľ���
					yEnd=event.getY();
					length=yEnd-yStart;
					isRecord=false;
					Log.v("length up",length+"   ");
					if(length>30&&isFirstItem){
						//���½������ݵļ��أ�Ч��̫�ͣ������������ʱ����кܴ����������
						loadContent(0,true);
						//Toast.makeText(Main.this,"ˢ����", Toast.LENGTH_LONG).show();
					}
					break;
				case(MotionEvent.ACTION_MOVE):
					//���ϼ�¼�͸���λ�ã�Ȼ������жϣ����λ�õ����˵�һ��������Ŀ����ʼ��¼�ƶ�����
					if(isFirstItem&&!isRecord){
						//��������һ���ô��ʼ��¼���λ��
						yStart=event.getY();
						isRecord=true;
						Log.v("yStart",yStart+"  ");	
					}
					break;
				}
				return false;
			}
		});
		
		//��������,��Ҫ���ж�listview�Ƿ���ʾ��һ��item
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
		
		//��һҳ��ť�Ĵ���
		next.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.v("nowPage",nowPage+"   ");
				pre.setVisibility(View.VISIBLE);
				if(nowPage>=pageCount-1){
					next.setText("û��");
					isLast=true;
				}else{
					nowPage++;
					now.setText("��"+nowPage+"ҳ");
					decideToShow();
				}
			}
		});
		
		//��һҳ��ť�Ĵ���
		pre.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(isLast){
					next.setText("��һҳ");
					isLast=false;
				}
				Log.v("nowPage",nowPage+"   ");
				// TODO Auto-generated method stub
				if(nowPage<2){
					pre.setVisibility(View.INVISIBLE);
				}else{
					nowPage--;
					now.setText("��"+nowPage+"ҳ");
					if(nowPage==1){
						pre.setVisibility(View.INVISIBLE);
					}
					decideToShow();
				}
			}
		});
		
		//spinner������
		final String[] data=new String[]{"ʱ��","���","�����"};
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
	
	//����ͬ������ʽ
	private void doSelect(int type){
		loadContent(type,true);
		Log.v("type",type+"  ");
	}
	
	//ȷ����Ҫ��ʾ������,��ʵ�����������������¿������ȷָ��ÿһҳҪ��ʾ�����ݣ�Ȼ����������ʾ����
	//��Ҫ�Ż�
	private void decideToShow(){
		//������������
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
	
	//ҳ����أ����ǵ����ݻ�ȡ���ӳ�����
	final Runnable content=new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			pd.dismiss();
			if(!result.isEmpty()){
				//initView();
				itemCount=result.size();
				//������䣬�о��ظ���
				pageCount=itemCount%showCount!=0?itemCount/showCount+1:itemCount/showCount;
				Log.v("pageCount",pageCount+"   ");
				decideToShow();
			}else{
				TextView tv=new TextView(Main.this);
				tv.setText("������");
				setContentView(tv);
			}
			init();
		}
		
	};
	
	//��ʼ��
	private void init(){
		now.setText("��һҳ");
		nowPage=1;
		pre.setVisibility(View.INVISIBLE);
		isLast=false;
		isFirstItem=true;
		isRecord=false;
	}
	
	//dialog����ʾ
	final Runnable dialog=new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			pd=ProgressDialog.show(Main.this,"","���ݼ�����", true);
		}
		
	};
	
	//���ݵ���ʾ��ˢ�£���Ҫ����һ���µ��߳̽������ݻ�ȡ
	//����ϴ�ƣ��ܼ򵥵�
	//����̫���runnable��������Ϊ����İ�׿�����ڷ�ui�߳̽���ui����
	//�е㵰�۰�
	private void loadContent(final int type,final boolean desc){
		Log.v("loadContent",type+"  "+desc);
		new Thread(){
			
			@Override
			public void run(){
				//���ؽ������Ի���
				h.post(dialog);
				try {
					Thread.currentThread().sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//���ݿ��ѯ������
				result=dh.query(type,desc);
				//����д�ͺö���
				h.post(content);
			}
		}.start();
	}
	
}
