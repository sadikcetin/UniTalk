package tr.org.uni_talk.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.pojo.Contact;

/**
 * Created by oem on 28.10.2017.
 */

public class AddingMemberAdapter extends ArrayAdapter<Contact>{

    TextView username=null;
    TextView userStatus=null;
    TextView userContactType=null;

    public ArrayList<Integer> selectedIds = new ArrayList<Integer>();

    public AddingMemberAdapter(Context context, int resource,List<Contact> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;


        if (view == null) {

            LayoutInflater inflater;
            inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.group_chat_item, null);

        }

        Contact contact = getItem(position);

        if(contact != null){

            username = (TextView)view.findViewById(R.id.textViewGroupUser);
            userStatus = (TextView)view.findViewById(R.id.textViewGroupStatus);
            userContactType = (TextView) view.findViewById(R.id.textViewContactType);
            userContactType.setVisibility(View.INVISIBLE);

            if(username != null && userStatus != null){
                username.setText(contact.getName());
                userStatus.setText(contact.isSelectable() ? contact.getStatus() : "Already Member");
            }
        }

        ImageView imageView = (ImageView)view.findViewById(R.id.contactImageView);

        if (convertView != null){
            if (selectedIds.contains(position)) {
                convertView.setSelected(true);
                convertView.setPressed(true);
                convertView.setBackgroundColor(Color.parseColor("#1E8BC3"));
                imageView.setImageResource(R.drawable.user_icon);
                username.setTextColor(Color.parseColor("#ffffff"));
                userStatus.setTextColor(Color.parseColor("#ffffff"));
            }
            else {
                convertView.setSelected(false);
                convertView.setPressed(false);
                convertView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                imageView.setImageResource(R.drawable.user_icon);
                username.setTextColor(Color.parseColor("#1E8BC3"));
                userStatus.setTextColor(Color.parseColor("#737373"));
            }

        }

        return  view;
    }

    public void toggleSelected(Integer position)
    {
        if(selectedIds.contains(position))
        {
            selectedIds.remove(position);
        }
        else
        {
            selectedIds.add(position);
        }
    }
}
