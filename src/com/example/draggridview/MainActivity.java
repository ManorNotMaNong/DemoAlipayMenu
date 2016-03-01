package com.example.draggridview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.example.draggridview.util.Const;
import com.example.draggridview.util.FileKit;
import com.example.draggridview.widget.BadgeView;
import com.example.draggridview.widget.DragGridView;
import com.example.draggridview.widget.DragGridView.OnChanageListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * 仿支付宝首页菜单编辑
 * 
 * @参考 DragGridView--xiaanming
 *     http://blog.csdn.net/xiaanming/article/details/17718579
 * 
 * @参考 BadgeView--Stefan Jauker https://github.com/kodex83/BadgeView
 *
 */
public class MainActivity extends Activity {
	private static final String ITEM_TEXT = "item_text";
	private static final String ITEM_IMAGE = "item_image";

	private List<HashMap<String, Object>> mMainMenuList;
	private List<HashMap<String, Object>> mMoreMenuList;

	private View mCurrentView;
	private DragGridView mDragGridView;

	private SimpleAdapter mSimpleAdapter;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 从本地读取 首页 menu列表
		mMainMenuList = (List<HashMap<String, Object>>) FileKit.getObject(this, Const.MENU_LIST_MAIN);
		if (mMainMenuList == null || !(mMainMenuList.size() > 0)) {
			mMainMenuList = new ArrayList<HashMap<String, Object>>();
			// 首页菜单不存在则初始化数据
			for (int i = 0; i < 9; i++) {
				HashMap<String, Object> itemHashMap = new HashMap<String, Object>();
				int resourceId = getResources().getIdentifier("q_" + i, "drawable", getPackageName());
				itemHashMap.put(ITEM_IMAGE, resourceId);
				itemHashMap.put(ITEM_TEXT, "拖拽 " + Integer.toString(i + 1));
				mMainMenuList.add(itemHashMap);
			}
			HashMap<String, Object> itemHashMap = new HashMap<String, Object>();
			itemHashMap.put(ITEM_TEXT, "更多");
			itemHashMap.put(ITEM_IMAGE, R.drawable.ic_launcher);
			mMainMenuList.add(itemHashMap);
		}
		// 从本地读取 更多 menu 列表
		mMoreMenuList = (List<HashMap<String, Object>>) FileKit.getObject(this, Const.MENU_LIST_MORE);
		if (mMoreMenuList == null) {
			mMoreMenuList = new ArrayList<HashMap<String, Object>>();
		}

		mSimpleAdapter = new SimpleAdapter(this, mMainMenuList, R.layout.grid_item,
				new String[] { ITEM_IMAGE, ITEM_TEXT }, new int[] { R.id.item_image, R.id.item_text });

		mDragGridView = (DragGridView) findViewById(R.id.dragGridView);
		// 置最后一个 item 是否可以拖拽
		mDragGridView.setLastCanDrag(false);
		// gridItem 拖动响应时间
		mDragGridView.setDragResponseMS(500l);
		// DragGridView暂时只能设置 SimpleAapter，ArrayAdapter 不能拖动，BaseAdapter 没有试
		mDragGridView.setAdapter(mSimpleAdapter);
		mDragGridView.setOnChangeListener(new OnChanageListener() {

			@Override
			public void onChange(int from, int to) {
				HashMap<String, Object> temp = mMainMenuList.get(from);
				// 交换item
				if (from < to) {
					for (int i = from; i < to; i++) {
						Collections.swap(mMainMenuList, i, i + 1);
					}
				} else if (from > to) {
					for (int i = from; i > to; i--) {
						Collections.swap(mMainMenuList, i, i - 1);
					}
				}
				mMainMenuList.set(to, temp);
				// 交换后移除被选择 item 的 badgeView
				resetLastView(mCurrentView);
				mSimpleAdapter.notifyDataSetChanged();

				// 保存交换后的首页菜单到本地
				FileKit.save(MainActivity.this, mMainMenuList, Const.MENU_LIST_MAIN);
			}

		});
		mDragGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// 如果已有 item 被选中，则重置它
				if (mCurrentView != null) {
					resetLastView(mCurrentView);
				} // 更多menu的入口
				else if (position == mMainMenuList.size() - 1) {
					Intent intent = new Intent(MainActivity.this, MoreMenuActivity.class);
					startActivity(intent);
				} // 其他menu入口
				else {
					Toast.makeText(parent.getContext(), position + 1 + "clicked", Toast.LENGTH_SHORT).show();
				}
			}

		});

		mDragGridView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				// 长按的不是最后一个 [更多] menu
				if (position != mMainMenuList.size() - 1) {
					resetLastView(mCurrentView);
					mCurrentView = view;
					// 为当前长按的 menu 添加 badgeView
					decorateCurrentView(parent, mCurrentView, position, id);

					Toast.makeText(view.getContext(), "long " + id, Toast.LENGTH_SHORT).show();
				}
				return true;
			}

		});

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onRestart() {
		super.onRestart();
		// mainActivity 重启后读取本地首页菜单列表，判断是否有更新
		List<HashMap<String, Object>> mainMenuList = (List<HashMap<String, Object>>) FileKit.getObject(this,
				Const.MENU_LIST_MAIN);
		if (mainMenuList != null) {
			if (mainMenuList.size() - mMainMenuList.size() > 0) {
				mMainMenuList = mainMenuList;
				// simpleAdapter 没有 add 方法，只能 gridView 重新绑定 adapter
				mSimpleAdapter = new SimpleAdapter(this, mMainMenuList, R.layout.grid_item,
						new String[] { ITEM_IMAGE, ITEM_TEXT }, new int[] { R.id.item_image, R.id.item_text });
				mDragGridView.setAdapter(mSimpleAdapter);

				// 更新更多 menu 列表
				mMoreMenuList = (List<HashMap<String, Object>>) FileKit.getObject(this, Const.MENU_LIST_MORE);
			}
		}
	}

	/**
	 * 点击其他 item 时，重置之前被选中的 item
	 * 
	 * @param view
	 */
	@SuppressWarnings("deprecation")
	private void resetLastView(View view) {
		if (view != null) {
			// view.setBackground(null);
			view.setBackgroundDrawable(null);
			ViewGroup parentContainer = (ViewGroup) view;
			ViewGroup badgeViewContainer = (ViewGroup) parentContainer.getChildAt(0);

			// 移除 badgeView
			badgeViewContainer.removeViewAt(1);
			mCurrentView = null;
		}
	}

	/**
	 * 长按 item 后，为 item 添加背景和标签，点击标签删除 item
	 * 
	 * @param parent
	 * @param view
	 * @param position
	 * @param id
	 */
	private void decorateCurrentView(AdapterView<?> parent, View view, final int position, long id) {
		if (view != null) {
			// 为 item 添加背景
			view.setBackgroundColor(Color.parseColor("#ABCDEF"));

			final View imageView = view.findViewById(R.id.item_image);
			BadgeView badgeView = new BadgeView(view.getContext());
			badgeView.setText("—");
			int sideLenPx = dip2px(view.getContext(), 15.f);
			badgeView.setHeight(sideLenPx);
			badgeView.setWidth(sideLenPx);
			badgeView.setGravity(Gravity.CENTER);
			badgeView.setTargetView(imageView);
			badgeView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					@SuppressWarnings("unchecked")
					HashMap<String, Object> item = (HashMap<String, Object>) mSimpleAdapter.getItem(position);
					mMoreMenuList.add(item);
					mMainMenuList.remove(item);
					resetLastView(mCurrentView);
					mSimpleAdapter.notifyDataSetChanged();
					String itemName = (String) item.get(ITEM_TEXT);
					Toast.makeText(v.getContext(), "delete!" + itemName, Toast.LENGTH_SHORT).show();

					// 保存编辑后的 menu
					FileKit.save(v.getContext(), mMoreMenuList, Const.MENU_LIST_MORE);
					FileKit.save(v.getContext(), mMainMenuList, Const.MENU_LIST_MAIN);
				}

			});
		}
	}

	/**
	 * dp 转 px
	 * 
	 * @param context
	 * @param dpValue
	 * @return
	 */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

}
