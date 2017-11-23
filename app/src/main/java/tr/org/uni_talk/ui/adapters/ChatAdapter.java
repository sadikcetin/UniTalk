package tr.org.uni_talk.ui.adapters;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.List;

import tr.org.uni_talk.R;
import tr.org.uni_talk.config.UniTalkConfig;
import tr.org.uni_talk.pojo.ChatMessage;

public class ChatAdapter extends AbstractBaseAdapter<ChatMessage> {

    private String TAG = this.getClass().getName();
    private int fontSize;

    public ChatAdapter(Activity activity) {

        super(activity);
        fontSize = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("font_size", "12"));
    }

    public ChatAdapter(Activity activity, List<ChatMessage> list) {
        super(activity, list);
        fontSize = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("font_size", "12"));

    }

    @Override
    public View getView(int position, final View convertView, final ViewGroup parent) {
        ChatMessage message = (ChatMessage) getItem(position);
        View vi = convertView;
        if (convertView == null)
            vi = getInflater().inflate(R.layout.chat_bubble, null);

        TextView msg = (TextView) vi.findViewById(R.id.message_text);
        msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        String text;

        ImageView image = (ImageView) vi.findViewById(R.id.message_image);

        // msg.setText(message.getBody() + "\n" + message.getTime());
        LinearLayout layout = (LinearLayout) vi
                .findViewById(R.id.bubble_layout);
        LinearLayout parent_layout = (LinearLayout) vi
                .findViewById(R.id.bubble_layout_parent);

        // if message is mine then align from right
        TextView textViewMsgTime = (TextView) vi.findViewById(R.id.textViewMsgTime);
        final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout.getLayoutParams();

        textViewMsgTime.setText(message.getTime());
        ImageView iw = (ImageView) vi.findViewById(R.id.imageViewRead);

        if (message.isMine()) {
            text = "<font color=#ffffff>" + message.getBody() + "</font>";
            if (message.isDelivered())
                iw.setImageResource(R.drawable.ic_done_all_24dp);
            else if (message.isSent())
                iw.setImageResource(R.drawable.ic_done_24dp);
            else
                iw.setImageResource(R.drawable.ic_info_black_24dp);

            if (!message.getMessageId().contains("MSG")) {
                msg.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                final File file = new File(Environment.getExternalStorageDirectory() + File.separator +
                        UniTalkConfig.INTERNAL.APP_DATA_FOLDER + File.separator + "Sent", message.getMessageId());
                RequestOptions requestOptions = new RequestOptions()
                        .placeholder(new ColorDrawable(Color.LTGRAY))
                        .transforms(new CenterCrop(), new RoundedCorners(24))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .dontAnimate();
                Glide.with(parent.getContext())
                        .load(file)
                        .apply(requestOptions)
                        .into(image);
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int id = v.getId();
                        if (id == R.id.message_image) {
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(file), "image/jpeg");
                            v.getContext().startActivity(intent);
                        }
                    }
                });
            } else {
                image.setVisibility(View.GONE);
                msg.setVisibility(View.VISIBLE);
                msg.setText(Html.fromHtml(text));
            }
            layout.setBackgroundResource(R.drawable.outgoing);
            parent_layout.setGravity(Gravity.RIGHT);
            params.setMargins(100, 25, 0, 0);
        }
        // If not mine then align from left
        else {
            text = "<font color=#ffffff>" + message.getBody() + "</font>";
            iw.setImageResource(0);
            if (!message.getMessageId().contains("MSG")) {
                msg.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                if (message.getMessageId().contains("IMG")) {
                    final File file = new File(Environment.getExternalStorageDirectory() + File.separator +
                            UniTalkConfig.INTERNAL.APP_DATA_FOLDER + File.separator + "Received", message.getMessageId());
                    RequestOptions requestOptions = new RequestOptions()
                            .placeholder(new ColorDrawable(Color.LTGRAY))
                            .transforms(new CenterCrop(), new RoundedCorners(24))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .dontAnimate();
                    Glide.with(parent.getContext())
                            .load(file)
                            .apply(requestOptions)
                            .into(image);

                    image.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int id = v.getId();
                            if (id == R.id.message_image) {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(file), "image/jpeg");
                                v.getContext().startActivity(intent);
                            }
                        }
                    });
                }
            } else {
                image.setImageResource(0);
                image.setVisibility(View.GONE);
                msg.setVisibility(View.VISIBLE);
                msg.setText(Html.fromHtml(text));
            }
            msg.setText(Html.fromHtml(text));
            layout.setBackgroundResource(R.drawable.incoming);
            parent_layout.setGravity(Gravity.LEFT);
            params.setMargins(0, 25, 100, 0);
        }

        return vi;
    }


    private ChatMessage findByMessageId(String msgID) {
        ChatMessage msg = null;
        for (int i = 0; i < getCount(); i++) {
            msg = (ChatMessage) getItem(i);
            if (msg.equals(msgID))
                return msg;
        }

        return msg;
    }

    public void delivered(String msgIdD) {
        ChatMessage msg = findByMessageId(msgIdD);

        if (msg != null) {
            msg.setDelivered(ChatMessage.MESSAGE_DELIVERED);
        }
    }

    public void sent(String msgId) {
        ChatMessage msg = findByMessageId(msgId);

        if (msg != null) {
            msg.setDelivered(ChatMessage.MESSAGE_SENT);
        }
    }
}
