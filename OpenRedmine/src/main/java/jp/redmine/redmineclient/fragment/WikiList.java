package jp.redmine.redmineclient.fragment;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.view.MenuItem;
import com.j256.ormlite.android.apptools.OrmLiteListFragment;

import java.sql.SQLException;

import jp.redmine.redmineclient.R;
import jp.redmine.redmineclient.activity.handler.IssueActionEmptyHandler;
import jp.redmine.redmineclient.activity.handler.IssueActionInterface;
import jp.redmine.redmineclient.adapter.RedmineVersionListAdapter;
import jp.redmine.redmineclient.adapter.RedmineWikiListAdapter;
import jp.redmine.redmineclient.db.cache.DatabaseCacheHelper;
import jp.redmine.redmineclient.db.cache.RedmineFilterModel;
import jp.redmine.redmineclient.entity.RedmineConnection;
import jp.redmine.redmineclient.entity.RedmineFilter;
import jp.redmine.redmineclient.entity.RedmineFilterSortItem;
import jp.redmine.redmineclient.entity.RedmineProjectVersion;
import jp.redmine.redmineclient.entity.RedmineWiki;
import jp.redmine.redmineclient.model.ConnectionModel;
import jp.redmine.redmineclient.param.ConnectionArgument;
import jp.redmine.redmineclient.param.ProjectArgument;
import jp.redmine.redmineclient.task.SelectProjectTask;
import jp.redmine.redmineclient.task.SelectWikiTask;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class WikiList extends OrmLiteListFragment<DatabaseCacheHelper> {
	private static final String TAG = WikiList.class.getSimpleName();
	private RedmineWikiListAdapter adapter;
	private SelectDataTask task;
	private View mFooter;
	private MenuItem menu_refresh;

	private IssueActionInterface mListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if(activity instanceof ActivityInterface){
			mListener = ((ActivityInterface)activity).getHandler( IssueActionInterface.class);
		}
		if(mListener == null) {
			//setup empty events
			mListener = new  IssueActionEmptyHandler();
		}

	}
	protected void cancelTask(){
		// cleanup task
		if(task != null && task.getStatus() == AsyncTask.Status.RUNNING){
			task.cancel(true);
		}
	}
	public WikiList(){
		super();
	}

	static public WikiList newInstance(ProjectArgument arg){
		WikiList fragment = new WikiList();
		fragment.setArguments(arg.getArgument());
		return fragment;
	}

	@Override
	public void onDestroyView() {
		cancelTask();
		setListAdapter(null);
		super.onDestroyView();
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setFastScrollEnabled(true);

		adapter = new RedmineWikiListAdapter(getHelper());
		ProjectArgument intent = new ProjectArgument();
		intent.setArgument(getArguments());
		adapter.setupParameter(intent.getConnectionId(),intent.getProjectId());
		adapter.notifyDataSetChanged();
		setListAdapter(adapter);


	}

	@Override
	public void onListItemClick(ListView listView, View v, int position, long id) {
		super.onListItemClick(listView, v, position, id);
		Object listitem = listView.getItemAtPosition(position);
		if(listitem == null || !RedmineWiki.class.isInstance(listitem)  )
		{
			return;
		}
		RedmineWiki item = (RedmineWiki) listitem;
		//TODO
	}

	private class SelectDataTask extends SelectWikiTask {
		public SelectDataTask(DatabaseCacheHelper helper, RedmineConnection con, long proj_id){
			super(helper,con,proj_id);
		}

		// can use UI thread here
		@Override
		protected void onPreExecute() {
			mFooter.setVisibility(View.VISIBLE);
			if(menu_refresh != null)
				menu_refresh.setEnabled(false);
			if(mPullToRefreshLayout != null && !mPullToRefreshLayout.isRefreshing())
				mPullToRefreshLayout.setRefreshing(true);
		}

		// can use UI thread here
		@Override
		protected void onPostExecute(Void b) {
			mFooter.setVisibility(View.GONE);
			adapter.notifyDataSetChanged();
			if(menu_refresh != null)
				menu_refresh.setEnabled(true);
			if(mPullToRefreshLayout != null)
				mPullToRefreshLayout.setRefreshComplete();
		}

		@Override
		protected void onProgress(int max, int proc) {
			adapter.notifyDataSetChanged();
			super.onProgress(max, proc);
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		mFooter = inflater.inflate(R.layout.listview_footer,null);
		mFooter.setVisibility(View.GONE);
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	private PullToRefreshLayout mPullToRefreshLayout;
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// This is the View which is created by ListFragment
		ViewGroup viewGroup = (ViewGroup) view;

		// We need to create a PullToRefreshLayout manually
		mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());

		// We can now setup the PullToRefreshLayout
		ActionBarPullToRefresh.from(getActivity())
				// We need to insert the PullToRefreshLayout into the Fragment's ViewGroup
				.insertLayoutInto(viewGroup)

				// We need to mark the ListView and it's Empty View as pullable
				// This is because they are not dirent children of the ViewGroup
				.theseChildrenArePullable(android.R.id.list, android.R.id.empty)

				// We can now complete the setup as desired
				.listener(new OnRefreshListener() {
					@Override
					public void onRefreshStarted(View view) {
						onRefresh();
					}
				})
				.setup(mPullToRefreshLayout);
	}
	protected void onRefresh(){
		if(task != null && task.getStatus() == AsyncTask.Status.RUNNING){
			return;
		}
		ProjectArgument intent = new ProjectArgument();
		intent.setArgument(getArguments());
		int id = intent.getConnectionId();
		ConnectionModel mConnection = new ConnectionModel(getActivity());
		RedmineConnection connection = mConnection.getItem(id);
		mConnection.finalize();
		task = new SelectDataTask(getHelper(), connection, (long)intent.getProjectId());
		task.execute("");
	}
}
