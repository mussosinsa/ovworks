package org.ovirt.engine.ui.uicommonweb.models.users;

import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicommonweb.validation.RegexValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class UserPasswordResetModel extends Model {
    private static final int MIN_PASSWORD_LENGTH = 12;

    private EntityModel<String> password;

    public EntityModel<String> getPassword() {
        return password;
    }

    private void setPassword(EntityModel<String> value) {
        password = value;
    }

    public UserPasswordResetModel() {
        setPassword(new EntityModel<String>());
        setTitle(ConstantsManager.getInstance().getConstants().resetPasswordTitle());
        setHashName("reset_password"); //$NON-NLS-1$
    }

    public boolean validate() {
        getPassword().validateEntity(new IValidation[] {
                new NotEmptyValidation(),
                new RegexValidation(
                        "^.{" + MIN_PASSWORD_LENGTH + ",}$", //$NON-NLS-1$ //$NON-NLS-2$
                        "패스워드는 최소 " + MIN_PASSWORD_LENGTH + "자리 이상이어야 합니다." //$NON-NLS-1$ //$NON-NLS-2$
                )
        });
        return getPassword().getIsValid();
    }
}
