package mnm.mods.tabbychat.extra.filters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import mnm.mods.tabbychat.api.filters.Filter;
import mnm.mods.tabbychat.api.filters.FilterSettings;
import mnm.mods.tabbychat.util.SoundHelper;
import mnm.mods.tabbychat.util.Translation;
import mnm.mods.util.Consumer;
import mnm.mods.util.gui.GuiButton;
import mnm.mods.util.gui.GuiCheckbox;
import mnm.mods.util.gui.GuiGridLayout;
import mnm.mods.util.gui.GuiLabel;
import mnm.mods.util.gui.GuiPanel;
import mnm.mods.util.gui.GuiText;
import mnm.mods.util.gui.events.ActionPerformed;
import mnm.mods.util.gui.events.GuiEvent;
import mnm.mods.util.gui.events.GuiKeyboardAdapter;
import mnm.mods.util.gui.events.GuiKeyboardEvent;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class GuiFilterEditor extends GuiPanel implements GuiKeyboardAdapter {

    private Filter filter;
    private Consumer<Filter> consumer;

    private GuiText txtName;
    private GuiCheckbox chkRemove;
    private GuiText txtDestinations;
    private GuiCheckbox chkSound;
    private GuiText txtSound;
    private GuiText txtPattern;
    private GuiLabel lblError;

    public GuiFilterEditor(Filter filter, Consumer<Filter> consumer) {
        this.setLayout(new GuiGridLayout(20, 15));
        this.filter = filter;
        this.consumer = consumer;

        Pattern pattern = filter.getPattern();
        FilterSettings settings = filter.getSettings();

        this.addComponent(new GuiLabel(Translation.FILTER_TITLE.translate()),
                new int[] { 8, 0, 1, 2 });

        this.addComponent(new GuiLabel(Translation.FILTER_NAME.translate()), new int[] { 1, 2, });
        this.addComponent(txtName = new GuiText(), new int[] { 5, 2, 10, 1 });
        txtName.setValue(filter.getName());

        this.addComponent(new GuiLabel(Translation.FILTER_DESTINATIONS.translate()),
                new int[] { 1, 5 });
        this.addComponent(txtDestinations = new GuiText(), new int[] { 10, 5, 10, 1 });
        txtDestinations.setValue(merge(filter.getSettings().getChannels()));
        txtDestinations.getTextField().setMaxStringLength(1000);

        this.addComponent(new GuiLabel(Translation.FILTER_HIDE.translate()), new int[] { 2, 7 });
        this.addComponent(chkRemove = new GuiCheckbox(), new int[] { 1, 7 });
        chkRemove.setValue(settings.isRemove());

        this.addComponent(new GuiLabel(Translation.FILTER_AUDIO_NOTIFY.translate()), new int[] { 2, 9 });
        this.addComponent(chkSound = new GuiCheckbox(), new int[] { 1, 9 });
        chkSound.setValue(settings.isSoundNotification());

        this.addComponent(txtSound = new GuiText(), new int[] { 3, 10, 14, 1 });
        txtSound.setValue(settings.getSoundName());
        txtSound.addKeyboardAdapter(new GuiKeyboardAdapter() {
            private int pos;
            @Override
            public void accept(GuiKeyboardEvent event) {
                final int max = 10;
                if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                    pos++;
                } else if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                    pos--;
                }

                // suggest sounds
                String val = txtSound.getValue().toLowerCase()
                        .substring(0, txtSound.getTextField().getCursorPosition());
                List<String> list = Lists.newArrayList();
                List<SoundHelper> sounds = SoundHelper.getSounds();
                for (SoundHelper s : sounds) {
                    if (s.getResource().toString().contains(val)) {
                        list.add(s.getResource().toString());
                    }
                }
                pos = Math.min(pos, list.size() - max);
                pos = Math.max(pos, 0);
                if (list.size() > max) {
                    list = list.subList(pos, pos + max);
                }
                txtSound.setHint(Joiner.on('\n').join(list));
                if ((Keyboard.isKeyDown(Keyboard.KEY_RETURN) || Keyboard.isKeyDown(Keyboard.KEY_NUMPADENTER))
                        && !list.isEmpty()) {
                    txtSound.setValue(list.get(0));
                    txtSound.setFocused(false);
                }
            }
        });

        GuiButton play = new GuiButton("\u25b6") {
            @Override
            public ResourceLocation getSound() {
                return new ResourceLocation(txtSound.getValue());
            }
        };
        this.addComponent(play, new int[] { 18, 10, 2, 1 });

        this.addComponent(new GuiLabel(Translation.FILTER_EXPRESSION.translate()),
                new int[] { 1, 13 });
        this.addComponent(txtPattern = new GuiText(), new int[] { 8, 13, 12, 1 });
        txtPattern.setValue(pattern == null ? "" : pattern.toString());

        this.addComponent(lblError = new GuiLabel(""), new int[] { 6, 14 });

        GuiButton accept = new GuiButton(I18n.format("gui.done"));
        accept.addActionListener(new ActionPerformed() {
            @Override
            public void action(GuiEvent event) {
                accept();
            }
        });
        this.addComponent(accept, new int[] { 1, 14, 4, 1 });

        GuiButton cancel = new GuiButton(I18n.format("gui.cancel"));
        cancel.addActionListener(new ActionPerformed() {
            @Override
            public void action(GuiEvent event) {
                cancel();
            }
        });
        this.addComponent(cancel, new int[] { 1, 15, 4, 1 });
    }

    private String merge(Set<String> set) {
        StringBuilder sb = new StringBuilder();
        for (String s : set) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    private Set<String> split(String s) {
        Set<String> set = new HashSet<String>();
        String[] split = s.split(",");
        for (String sp : split) {
            sp = sp.trim();
            if (!sp.isEmpty()) {
                set.add(sp);
            }
        }
        return set;
    }

    private void accept() {
        filter.setName(txtName.getValue());
        filter.setPattern(txtPattern.getValue());
        FilterSettings sett = filter.getSettings();
        sett.getChannels().addAll(split(txtDestinations.getValue()));
        sett.setRemove(chkRemove.getValue());

        sett.setSoundNotification(chkSound.getValue());
        sett.setSoundName(txtSound.getValue());

        consumer.apply(filter);
        cancel();
    }

    private void cancel() {
        getParent().setOverlay(null);
    }

    @Override
    public void accept(GuiKeyboardEvent event) {
        if (txtPattern.isFocused()) {
            // check valid regex
            try {
                Pattern.compile(txtPattern.getValue());
                txtPattern.setForeColor(-1);
                lblError.setString("");
            } catch (PatternSyntaxException e) {
                txtPattern.setForeColor(0xffff0000);
                String string = e.getLocalizedMessage();
                lblError.setString(string);
            }
        }
    }
}
