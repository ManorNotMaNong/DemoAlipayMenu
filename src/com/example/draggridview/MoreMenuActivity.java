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
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class MoreMenuActivity extends Activity {
	private static final String ITEM_TEXT = "item_text";
	private static final String ITEM_IMAGE = "item_image";

	private List<HashMap<String, Object>> mMoreMenuList;
	private List<HashMap<String, Object>> mMainMenuList;

	private DragGridView mDragGridView;
	private View mCurrentView;
	private SimpleAdapter mSimpleAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initView();
	}

	@SuppressWarnings("unchecked")
	private void initView() {
		setContentView(R.layout.activity_more_menu);

		mDragGridView = (DragGridView) findViewById(R.id.more_menu_drag_grid_view);

		// 读取本地首页 menu列表
		mMainMenuList = (List<HashMap<String, Object>>) FileKit.getObject(this, Const.MENU_LIST_MAIN);
		if (mMainMenuList == null) {
			mMainMenuList = new ArrayList<HashMap<String, Object>>();
		}
		// 读取本地更多 menu列表
		mMoreMenuList = (List<HashMap<String, Object>>) FileKit.getObject(this, Const.MENU_LIST_MORE);
		if (mMoreMenuList == null) {
			mMoreMenuList = new ArrayList<HashMap<String, Object>>();
		}
		Log.d("leo", "Second more menu size:" + mMoreMenuList.size());

		mSimpleAdapter = new SimpleAdapter(this, mMoreMenuList, R.layout.grid_item,
				new String[] { ITEM_IMAGE, ITEM_TEXT }, new int[] { R.id.item_image, R.id.item_text });
		// DragGridView暂时只能设置 SimpleAapter，ArrayAdapter 不能拖动，BaseAdapter 没有试
		mDragGridView.setAdapter(mSimpleAdapter);
		mDragGridView.setOnChangeListener(new OnChanageListener() {

			@Override
			public void onChange(int from, int to) {
				HashMap<String, Object> temp = mMoreMenuList.get(from);
				// 交换item
				if (from < to) {
					for (int i = from; i < to; i++) {
						Collections.swap(mMoreMenuList, i, i + 1);
					}
				} else if (from > to) {
					for (int i = from; i > to; i--) {
						Collections.swap(mMoreMenuList, i, i - 1);
					}
				}
				mMoreMenuList.set(to, temp);
				resetLastView(mCurrentView);
				mSimpleAdapter.notifyDataSetChanged();
				// 将交换后的menu列表保存到本地
				FileKit.save(MoreMenuActivity.this, mMoreMenuList, Const.MENU_LIST_MORE);
			}
		});

		mDragGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				if (mCurrentView != null) {
					resetLastView(mCurrentView);
				} // menu 入口
				else {
					Toast.makeText(parent.getContext(), position + 1 + "clicked", Toast.LENGTH_SHORT).show();
				}
			}

		});

		mDragGridView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				resetLastView(mCurrentView);
				mCurrentView = view;
				decorateCurrentView(parent, mCurrentView, position, id);
				Toast.makeText(view.getContext(), "long " + id, Toast.LENGTH_SHORT).show();
				return true;
			}

		});

	}

	/**
	 * 点击其他 item 时，重置之前被选中的 item:去掉右上角的 badgeView
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
			view.setBackgroundColor(Color.parseColor("#ABCDEF"));
			final View imageView = view.findViewById(R.id.item_image);
			BadgeView badgeView = new BadgeView(view.getContext());
			badgeView.setText("+");
			int sideLenPx = dip2px(view.getContext(), 15.f);
			badgeView.setHeight(sideLenPx);
			badgeView.setWidth(sideLenPx);
			badgeView.setGravity(Gravity.CENTER);
			badgeView.setBackground(8, Color.GREEN);
			badgeView.setTargetView(imageView);
			badgeView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					@SuppressWarnings("unchecked")
					HashMap<String, Object> item = (HashMap<String, Object>) mSimpleAdapter.getItem(position);
					// 插入到更多 menu 之前
					mMainMenuList.add((mMainMenuList.size() - 1), item);
					mMoreMenuList.remove(item);
					resetLastView(mCurrentView);
					mSimpleAdapter.notifyDataSetChanged();

					String itemName = (String) item.get("item_text");
					Toast.makeText(v.getContext(), "delete!" + itemName, Toast.LENGTH_SHORT).show();

					FileKit.save(v.getContext(), mMainMenuList, Const.MENU_LIST_MAIN);
					FileKit.save(v.getContext(), mMoreMenuList, Const.MENU_LIST_MORE);
				}

			});
		}
	}

	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

}
