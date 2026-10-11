package com.github.warren_bank.bookmarks.ui.dialogs;

import com.github.warren_bank.bookmarks.R;
import com.github.warren_bank.bookmarks.database.DbGateway;
import com.github.warren_bank.bookmarks.security.PrivateLock;
import com.github.warren_bank.bookmarks.ui.dialogs.FolderContentsPicker;
import com.github.warren_bank.bookmarks.ui.model.FolderContentItem;
import com.github.warren_bank.bookmarks.ui.widgets.FolderContentsAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.List;

public class DbFolderPicker {

  public interface Listener extends FolderContentsPicker.Listener {
  }

  public static void pickFolder(Context context, FolderContentsPicker.Listener listener, int initialFolderId) {
    boolean allowPrivateWhenUnlocked = false;
    DbFolderPicker.showFilePicker(context, listener, initialFolderId, /* showFiles */ false, allowPrivateWhenUnlocked);
  }

  public static void pickFolder(Context context, FolderContentsPicker.Listener listener, int initialFolderId, boolean allowPrivateWhenUnlocked) {
    DbFolderPicker.showFilePicker(context, listener, initialFolderId, /* showFiles */ false, allowPrivateWhenUnlocked);
  }

  public static void pickFile(Context context, FolderContentsPicker.Listener listener, int initialFolderId) {
    boolean allowPrivateWhenUnlocked = false;
    DbFolderPicker.showFilePicker(context, listener, initialFolderId, /* showFiles */ true, allowPrivateWhenUnlocked);
  }

  public static void showFilePicker(Context context, FolderContentsPicker.Listener listener, int initialFolderId, boolean showFiles) {
    boolean allowPrivateWhenUnlocked = false;
    DbFolderPicker.showFilePicker(context, listener, initialFolderId, showFiles, allowPrivateWhenUnlocked);
  }

  public static void showFilePicker(Context context, FolderContentsPicker.Listener listener, int folderId, boolean showFiles, int resId_pickFolderPositiveButton) {
    boolean allowPrivateWhenUnlocked = false;
    DbFolderPicker.showFilePicker(context, listener, folderId, showFiles, resId_pickFolderPositiveButton, allowPrivateWhenUnlocked);
  }

  public static void showFilePicker(Context context, FolderContentsPicker.Listener listener, int folderId, boolean showFiles, int resId_pickFolderPositiveButton, boolean allowPrivateWhenUnlocked) {
    FolderContentItem initialFolder = DbGateway.getInstance(context).getFolderContentItem(folderId);
    DbFolderPicker.showFilePicker(context, new DbFolderPickerImplementation(context), listener, initialFolder, showFiles, resId_pickFolderPositiveButton, allowPrivateWhenUnlocked);
  }

  public static class DbFolderPickerImplementation implements FolderContentsPicker.Implementation {
    private final Context context;

    public DbFolderPickerImplementation(Context context) {
      this.context = context;
    }

    @Override
    public FolderContentItem getParentFolderContentItem(FolderContentItem folder) {
      return DbGateway.getInstance(context).getParentFolderContentItem(folder.id);
    }

    @Override
    public List<FolderContentItem> getFolderContentItems(FolderContentItem folder, boolean showFiles) {
      boolean includeURL = showFiles;
      return DbGateway.getInstance(context).getFolderContentItems(folder.id, includeURL);
    }
  }

  public static void showFilePicker(Context context, FolderContentsPicker.Listener listener, int folderId, boolean showFiles, boolean allowPrivateWhenUnlocked) {
    int resId_pickFolderPositiveButton = R.string.dialog_ok;
    DbFolderPicker.showFilePicker(context, listener, folderId, showFiles, resId_pickFolderPositiveButton, allowPrivateWhenUnlocked);
  }

  public static void showFilePicker(final Context context, final FolderContentsPicker.Implementation implementation, final FolderContentsPicker.Listener listener, FolderContentItem currentFolder, boolean showFiles, int resId_pickFolderPositiveButton, final boolean allowPrivateWhenUnlocked) {
    final boolean includePrivate = allowPrivateWhenUnlocked && PrivateLock.getInstance(context).isUnlocked();

    FolderContentItem parentFolder = implementation.getParentFolderContentItem(currentFolder);
    List<FolderContentItem> items = implementation.getFolderContentItems(currentFolder, showFiles);

    // hide the password-protected private folder (and anything inside it) unless it has been unlocked in this session
    if (!includePrivate) {
      for (int i = items.size() - 1; i >= 0; i--) {
        if (items.get(i).isPrivate)
          items.remove(i);
      }
    }
    if (parentFolder != null) {
      items.add(0, new FolderContentItem(
        /* id       */ parentFolder.id,
        /* name     */ "..",
        /* isFolder */ true,
        /* isHidden */ true
      ));
    }

    FolderContentsAdapter adapter = new FolderContentsAdapter(context, items, /* showHidden */ true);

    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context)
      .setTitle(currentFolder.name)
      .setAdapter(adapter, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          FolderContentItem item = adapter.getItem(which);

          if (item.isFolder) {
            if (item.isHidden && item.name.equals(".."))
              item = parentFolder;

            listener.onFolderChange(item);
            DbFolderPicker.showFilePicker(context, implementation, listener, /* folder */ item, showFiles, resId_pickFolderPositiveButton, allowPrivateWhenUnlocked);
          }
          else {
            listener.onFilePick(item);
          }
        }
      })
      .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          dialog.dismiss();
        }
      });

    if (!showFiles) {
      dialogBuilder.setPositiveButton(resId_pickFolderPositiveButton, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          listener.onFolderPick(currentFolder);
        }
      });
    }

    AlertDialog filePicker = dialogBuilder.show();

    if (!showFiles && (!listener.isValidFolderToPick(currentFolder) || (currentFolder.isPrivate && !includePrivate))) {
      filePicker.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
    }
  }
}
