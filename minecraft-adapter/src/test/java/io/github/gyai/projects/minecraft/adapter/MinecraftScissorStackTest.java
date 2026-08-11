package io.github.gyai.projects.minecraft.adapter;

import io.github.gyai.projects.ui.runtime.UiDrawList;
import io.github.gyai.projects.ui.runtime.UiRect;
import io.github.gyai.projects.ui.runtime.UiRenderCommand;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fStack;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.List;

/** Non-GUI evidence for logical/native nested scissor ownership. */
public final class MinecraftScissorStackTest {
    private static final Unsafe UNSAFE = unsafe();
    private static final Method RENDER_COMMAND = renderCommand();

    private MinecraftScissorStackTest() { }

    public static void main(String[] args) throws Exception {
        run();
    }

    static void run() throws Exception {
        directEmptyClipLeavesNativeStateEmpty();
        disjointNestedEmptyClipRestoresParent();
        externalParentSurvivesEmptyNestedClip();
        nestedEmptyDescendantsRemainBalanced();
        singlePushPopReturnsToInitialState();
        twoLevelsRestoreParentThenInitial();
        threeLevelsRestoreEachParent();
        siblingClipsDoNotLeak();
        emptyStackBehaviorMatchesContract();
        logicalIntersectionMatchesNativeState();
        System.out.println("SCISSOR_STACK_TEST_PASS: native scissor stack restores nested sibling and empty clips");
    }

    private static void directEmptyClipLeavesNativeStateEmpty() throws Exception {
        TestContext context = context();
        UiRenderCommand.PushClip emptyPush = new UiRenderCommand.PushClip(UiRect.empty());

        apply(context, emptyPush);
        expectCurrent(context, null, "direct empty push does not add a native level");
        apply(context, new UiRenderCommand.PopClip());
        expectCurrent(context, null, "direct empty pop does not remove a native level");
    }

    private static void disjointNestedEmptyClipRestoresParent() throws Exception {
        UiRect a = new UiRect(0, 0, 20, 20);
        UiRect disjoint = new UiRect(40, 40, 10, 10);
        UiDrawList drawList = new UiDrawList();
        UiRect effectiveA = drawList.pushClip(a);
        UiRect effectiveEmpty = drawList.pushClip(disjoint);
        drawList.popClip();
        drawList.popClip();
        drawList.assertBalanced();
        check(effectiveEmpty.isEmpty(), "disjoint nested clip is logically empty");

        TestContext context = context();
        List<UiRenderCommand> commands = drawList.commands();
        apply(context, commands.get(0));
        expectCurrent(context, effectiveA, "non-empty parent creates one native level");
        apply(context, commands.get(1));
        expectCurrent(context, effectiveA, "empty nested intersection does not create a native level");
        apply(context, commands.get(2));
        expectCurrent(context, effectiveA, "empty nested pop preserves the native parent");
        apply(context, commands.get(3));
        expectCurrent(context, null, "parent pop returns the native stack to empty");
    }

    private static void externalParentSurvivesEmptyNestedClip() throws Exception {
        TestContext context = context();
        UiRect initial = new UiRect(10, 20, 100, 80);
        MinecraftScissorBridge bridge = new MinecraftScissorBridge();
        bridge.push(context.graphics(), initial);

        UiDrawList drawList = new UiDrawList();
        drawList.pushClip(UiRect.empty());
        drawList.popClip();
        drawList.assertBalanced();
        context.backend().render(drawList);
        expectCurrent(context, initial, "public empty clip render preserves the external native parent");

        bridge.pop(context.graphics());
        expectCurrent(context, null, "external parent can be popped explicitly after empty render");
    }

    private static void nestedEmptyDescendantsRemainBalanced() throws Exception {
        UiRect a = new UiRect(0, 0, 20, 20);
        UiRect disjointB = new UiRect(40, 40, 10, 10);
        UiRect disjointC = new UiRect(60, 60, 10, 10);
        UiDrawList drawList = new UiDrawList();
        UiRect effectiveA = drawList.pushClip(a);
        UiRect effectiveB = drawList.pushClip(disjointB);
        UiRect effectiveC = drawList.pushClip(disjointC);
        drawList.popClip();
        drawList.popClip();
        drawList.popClip();
        drawList.assertBalanced();
        check(effectiveB.isEmpty() && effectiveC.isEmpty(), "empty descendants remain logically empty");

        TestContext publicContext = context();
        publicContext.backend().render(drawList);
        expectCurrent(publicContext, null, "public nested empty render returns to the initial native state");

        TestContext stepContext = context();
        List<UiRenderCommand> commands = drawList.commands();
        apply(stepContext, commands.get(0));
        expectCurrent(stepContext, effectiveA, "nested empty sequence starts with parent A");
        apply(stepContext, commands.get(1));
        expectCurrent(stepContext, effectiveA, "empty descendant B does not alter native state");
        apply(stepContext, commands.get(2));
        expectCurrent(stepContext, effectiveA, "empty descendant C does not alter native state");
        apply(stepContext, commands.get(3));
        expectCurrent(stepContext, effectiveA, "pop C preserves native parent A");
        apply(stepContext, commands.get(4));
        expectCurrent(stepContext, effectiveA, "pop B preserves native parent A");
        apply(stepContext, commands.get(5));
        expectCurrent(stepContext, null, "pop A finally empties the native stack");
    }

    private static void singlePushPopReturnsToInitialState() throws Exception {
        TestContext context = context();
        UiRect initial = new UiRect(10, 20, 100, 80);
        UiRect clip = new UiRect(50, 40, 100, 80);
        MinecraftScissorBridge bridge = new MinecraftScissorBridge();

        bridge.push(context.graphics(), initial);
        expectCurrent(context, initial, "external native scissor state is preserved as the initial state");
        apply(context, new UiRenderCommand.PushClip(clip));
        expectCurrent(context, initial.intersection(clip), "single push installs the nested clip");
        apply(context, new UiRenderCommand.PopClip());
        expectCurrent(context, initial, "single pop restores the initial state");
        bridge.pop(context.graphics());
        expectCurrent(context, null, "the external parent can then restore the empty state");
    }

    private static void twoLevelsRestoreParentThenInitial() throws Exception {
        TestContext context = context();
        UiRect a = new UiRect(10, 20, 100, 80);
        UiRect b = new UiRect(40, 40, 30, 20);

        apply(context, new UiRenderCommand.PushClip(a));
        apply(context, new UiRenderCommand.PushClip(b));
        expectCurrent(context, a.intersection(b), "nested push uses the native intersection");
        apply(context, new UiRenderCommand.PopClip());
        expectCurrent(context, a, "pop B restores A exactly once");
        apply(context, new UiRenderCommand.PopClip());
        expectCurrent(context, null, "pop A restores the initial state");
    }

    private static void threeLevelsRestoreEachParent() throws Exception {
        TestContext context = context();
        UiRect a = new UiRect(0, 0, 120, 100);
        UiRect b = new UiRect(10, 15, 80, 70);
        UiRect c = new UiRect(30, 25, 25, 20);

        apply(context, new UiRenderCommand.PushClip(a));
        apply(context, new UiRenderCommand.PushClip(b));
        apply(context, new UiRenderCommand.PushClip(c));
        expectCurrent(context, a.intersection(b).intersection(c), "three nested pushes intersect");
        apply(context, new UiRenderCommand.PopClip());
        expectCurrent(context, a.intersection(b), "pop C restores B");
        apply(context, new UiRenderCommand.PopClip());
        expectCurrent(context, a, "pop B restores A");
        apply(context, new UiRenderCommand.PopClip());
        expectCurrent(context, null, "pop A restores the initial state");
    }

    private static void siblingClipsDoNotLeak() throws Exception {
        TestContext context = context();
        UiRect a = new UiRect(0, 0, 100, 100);
        UiRect b = new UiRect(10, 10, 20, 20);
        UiRect c = new UiRect(60, 60, 20, 20);

        apply(context, new UiRenderCommand.PushClip(a));
        apply(context, new UiRenderCommand.PushClip(b));
        apply(context, new UiRenderCommand.PopClip());
        expectCurrent(context, a, "sibling setup restores A before C");
        apply(context, new UiRenderCommand.PushClip(c));
        expectCurrent(context, c, "sibling C is not intersected with stale B");
        apply(context, new UiRenderCommand.PopClip());
        apply(context, new UiRenderCommand.PopClip());
        expectCurrent(context, null, "sibling sequence returns to the initial state");
    }

    private static void emptyStackBehaviorMatchesContract() throws Exception {
        UiDrawList drawList = new UiDrawList();
        try {
            drawList.popClip();
            throw new AssertionError("logical empty clip pop must fail");
        } catch (IllegalStateException error) {
            check("Cannot pop an empty clip stack".equals(error.getMessage()),
                    "logical empty clip pop message");
        }

        TestContext context = context();
        try {
            apply(context, new UiRenderCommand.PopClip());
            throw new AssertionError("adapter empty clip pop must fail");
        } catch (IllegalStateException error) {
            check("Clip pop without push".equals(error.getMessage()), "adapter empty clip pop message");
        }
        expectCurrent(context, null, "adapter rejects an empty pop before touching native state");
    }

    private static void logicalIntersectionMatchesNativeState() throws Exception {
        UiRect requestedA = new UiRect(10, 20, 100, 80);
        UiRect requestedB = new UiRect(50, 40, 100, 80);
        UiDrawList drawList = new UiDrawList();
        UiRect effectiveA = drawList.pushClip(requestedA);
        UiRect effectiveB = drawList.pushClip(requestedB);
        drawList.popClip();
        drawList.popClip();
        drawList.assertBalanced();

        check(effectiveA.equals(requestedA), "first logical clip remains unchanged");
        check(effectiveB.equals(requestedA.intersection(requestedB)), "nested logical clip intersects its parent");

        TestContext publicContext = context();
        publicContext.backend().render(drawList);
        expectCurrent(publicContext, null, "public backend render leaves native state balanced");

        TestContext stepContext = context();
        List<UiRenderCommand> commands = drawList.commands();
        check(commands.size() == 4, "logical clip sequence contains two pushes and two pops");
        apply(stepContext, commands.get(0));
        expectCurrent(stepContext, effectiveA, "native state matches logical clip A");
        apply(stepContext, commands.get(1));
        expectCurrent(stepContext, effectiveB, "native state matches logical nested intersection");
        apply(stepContext, commands.get(2));
        expectCurrent(stepContext, effectiveA, "native pop restores logical clip A");
        apply(stepContext, commands.get(3));
        expectCurrent(stepContext, null, "native final pop restores the initial state");
    }

    private static TestContext context() throws Exception {
        GuiGraphicsExtractor graphics = graphicsWithoutMinecraft();
        return new TestContext(graphics, new MinecraftUiRenderBackend(graphics, null));
    }

    private static GuiGraphicsExtractor graphicsWithoutMinecraft() throws Exception {
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) UNSAFE.allocateInstance(GuiGraphicsExtractor.class);
        Matrix3x2fStack pose = new Matrix3x2fStack(16);
        pose.identity();
        setReference(graphics, "pose", pose);

        GuiGraphicsExtractor.ScissorStack scissorStack =
                (GuiGraphicsExtractor.ScissorStack) UNSAFE.allocateInstance(GuiGraphicsExtractor.ScissorStack.class);
        setReference(scissorStack, "stack", new ArrayDeque<ScreenRectangle>());
        setReference(graphics, "scissorStack", scissorStack);
        return graphics;
    }

    private static void apply(TestContext context, UiRenderCommand command) throws Exception {
        try {
            RENDER_COMMAND.invoke(context.backend(), command);
        } catch (InvocationTargetException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error failure) throw failure;
            throw new AssertionError(cause);
        }
    }

    private static void expectCurrent(TestContext context, UiRect expected, String message) {
        ScreenRectangle actual = context.graphics().scissorStack.peek();
        ScreenRectangle expectedNative = expected == null ? null : nativeRectangle(expected);
        check(expectedNative == null ? actual == null : expectedNative.equals(actual),
                message + " (actual=" + actual + ", expected=" + expectedNative + ")");
    }

    private static ScreenRectangle nativeRectangle(UiRect rect) {
        int left = (int) Math.floor(rect.x());
        int top = (int) Math.floor(rect.y());
        int right = (int) Math.ceil(rect.right());
        int bottom = (int) Math.ceil(rect.bottom());
        return new ScreenRectangle(left, top, right - left, bottom - top);
    }

    private static Method renderCommand() {
        try {
            Method method = MinecraftUiRenderBackend.class.getDeclaredMethod("render", UiRenderCommand.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static void setReference(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        UNSAFE.putObject(target, UNSAFE.objectFieldOffset(field), value);
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record TestContext(GuiGraphicsExtractor graphics, MinecraftUiRenderBackend backend) { }
}
